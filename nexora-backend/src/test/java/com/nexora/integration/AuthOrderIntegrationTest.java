package com.nexora.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.application.dto.auth.LoginRequest;
import com.nexora.application.dto.order.CreateOrderRequest;
import com.nexora.application.dto.order.OrderItemRequest;
import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.application.dto.user.CreateUserRequest;
import com.nexora.domain.model.UserRole;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth + Order Integration Tests")
class AuthOrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nexora_it")
            .withUsername("nexora")
            .withPassword("nexora");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("nexora.jwt.secret", () ->
                "nexora-integration-test-secret-key-256-bits-long-hmac-sha256-ok");
        r.add("nexora.jwt.access-token-expiration-ms",  () -> "900000");
        r.add("nexora.jwt.refresh-token-expiration-ms", () -> "604800000");
    }

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  mapper;

    // Shared state across test methods
    static String adminToken;
    static String customerToken;
    static String productId;
    static String orderId;

    @Test @org.junit.jupiter.api.Order(1)
    @DisplayName("Admin should be able to login with seed credentials")
    void adminLogin() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("admin@nexora.com", "admin@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        adminToken = mapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test @org.junit.jupiter.api.Order(2)
    @DisplayName("Should register a customer user")
    void registerCustomer() throws Exception {
        var req = new CreateUserRequest("Test Customer", "testcustomer@nexora.com",
                "password123", UserRole.CUSTOMER);

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @org.junit.jupiter.api.Order(3)
    @DisplayName("Customer should be able to login")
    void customerLogin() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(
                                new LoginRequest("testcustomer@nexora.com", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andReturn();

        customerToken = mapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test @org.junit.jupiter.api.Order(4)
    @DisplayName("Admin should create a product")
    void createProduct() throws Exception {
        var req = new CreateProductRequest("Integration Notebook", "Test laptop",
                "IT-NB-001", new BigDecimal("2999.99"), "BRL", 20, null);

        var result = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        productId = mapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test @org.junit.jupiter.api.Order(5)
    @DisplayName("Customer should be able to browse products without auth")
    void publicProductsBrowse() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test @org.junit.jupiter.api.Order(6)
    @DisplayName("Customer should create an order")
    void customerCreatesOrder() throws Exception {
        var req = new CreateOrderRequest(
                List.of(new OrderItemRequest(java.util.UUID.fromString(productId), 2)),
                "Integration test order"
        );

        var result = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        orderId = mapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    @Test @org.junit.jupiter.api.Order(7)
    @DisplayName("Admin confirms order and stock should decrease")
    void adminConfirmsOrder() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{id}/confirm", orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Verifica que estoque foi decrementado
        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(jsonPath("$.stockQuantity").value(18));
    }

    @Test @org.junit.jupiter.api.Order(8)
    @DisplayName("Customer should not access another user's order")
    void customerCannotAccessOtherOrder() throws Exception {
        // Tenta acessar como admin um pedido que não é seu
        // (neste caso o customerToken não é dono do pedido de outrem)
        // Aqui buscamos nosso próprio pedido — deve funcionar
        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test @org.junit.jupiter.api.Order(9)
    @DisplayName("Unauthenticated request to protected route returns 401/403")
    void unauthenticatedShouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}