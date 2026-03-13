package com.nexora.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.application.dto.auth.LoginRequest;
import com.nexora.application.dto.product.CreateProductRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração com Testcontainers + PostgreSQL real.
 * Flyway é habilitado via @DynamicPropertySource para que os seeds sejam executados.
 * Kafka e Redis são desabilitados via application.yml de teste.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Product Integration Tests")
class ProductIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nexora_it")
            .withUsername("nexora")
            .withPassword("nexora");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",            postgres::getJdbcUrl);
        r.add("spring.datasource.username",        postgres::getUsername);
        r.add("spring.datasource.password",        postgres::getPassword);
        r.add("spring.flyway.enabled",             () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto",     () -> "validate");
        r.add("nexora.jwt.secret", () ->
                "nexora-integration-test-secret-key-256-bits-long-hmac-sha256-ok");
        r.add("nexora.jwt.access-token-expiration-ms",  () -> "900000");
        r.add("nexora.jwt.refresh-token-expiration-ms", () -> "604800000");
    }

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  mapper;

    static String adminToken;

    @Test @org.junit.jupiter.api.Order(1)
    @DisplayName("Admin should login and get JWT token")
    void adminLogin() throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new LoginRequest("admin@nexora.com", "admin@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        adminToken = mapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test @org.junit.jupiter.api.Order(2)
    @DisplayName("Should create and retrieve a product end-to-end")
    void shouldCreateAndRetrieveProduct() throws Exception {
        var request = new CreateProductRequest(
                "Integration Laptop", "Test laptop for integration test",
                "IT-LAP-001", new BigDecimal("2999.99"), "BRL", 10, null  // categoryId = null
        );

        var createResult = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("IT-LAP-001"))
                .andExpect(jsonPath("$.stockQuantity").value(10))
                .andReturn();

        var id = mapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // GET (público)
        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Laptop"))
                .andExpect(jsonPath("$.active").value(true));

        // Soft delete
        mockMvc.perform(delete("/api/v1/products/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Ainda retornável, mas inativo
        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test @org.junit.jupiter.api.Order(3)
    @DisplayName("GET /api/v1/products should be public and return pagination")
    void publicProductListShouldReturnPage() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test @org.junit.jupiter.api.Order(4)
    @DisplayName("Should return 409 on duplicate SKU")
    void shouldRejectDuplicateSku() throws Exception {
        var request = new CreateProductRequest(
                "Dup Product", "Desc", "DUP-SKU-IT", BigDecimal.TEN, "BRL", 1, null
        );

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}