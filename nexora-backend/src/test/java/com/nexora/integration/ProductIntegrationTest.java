package com.nexora.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.application.dto.product.CreateProductRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Teste de integração completo com Testcontainers.
 * Melhoria: banco PostgreSQL real em container Docker durante os testes.
 * Valida todo o stack: Controller → UseCase → Repository → PostgreSQL.
 * <p>
 * Requer Docker instalado. Pule com -x integrationTest se não disponível.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Product Integration Tests (Testcontainers)")
class ProductIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nexora_test")
            .withUsername("nexora")
            .withPassword("nexora");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should create and retrieve a product end-to-end")
    void shouldCreateAndRetrieveProduct() throws Exception {
        var request = new CreateProductRequest(
                "Integration Product", "End-to-end test product",
                "IT-001", new BigDecimal("299.99"), "BRL", 5
        );

        // Create
        var createResult = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("IT-001"))
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andReturn();

        // Extract ID from response
        var responseBody = objectMapper.readTree(createResult.getResponse().getContentAsString());
        var id = responseBody.get("id").asText();

        // Retrieve
        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Product"))
                .andExpect(jsonPath("$.active").value(true));

        // Soft delete
        mockMvc.perform(delete("/api/v1/products/{id}", id))
                .andExpect(status().isNoContent());

        // Verify still retrievable but inactive
        mockMvc.perform(get("/api/v1/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("Should return 409 on duplicate SKU")
    void shouldRejectDuplicateSku() throws Exception {
        var request = new CreateProductRequest(
                "Duplicate SKU Product", "Test", "DUP-SKU-001",
                BigDecimal.TEN, "BRL", 1
        );

        // First creation — OK
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second creation with same SKU — Conflict
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}