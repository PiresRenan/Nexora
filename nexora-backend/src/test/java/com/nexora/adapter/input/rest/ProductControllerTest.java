package com.nexora.adapter.input.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.application.dto.product.ProductResponse;
import com.nexora.application.usecase.ProductUseCase;
import com.nexora.domain.exception.DuplicateResourceException;
import com.nexora.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de slice do controller.
 * Melhoria: @WebMvcTest carrega apenas a camada web — mais rápido que @SpringBootTest.
 * O useCase é mockado, isolando o teste apenas ao comportamento do controller.
 */
@WebMvcTest(ProductController.class)
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductUseCase productUseCase;

    private ProductResponse sampleResponse() {
        return new ProductResponse(
                UUID.fromString("b0000000-0000-0000-0000-000000000001"),
                "Notebook Pro", "High performance laptop", "NB-001",
                new BigDecimal("4999.99"), "BRL",
                10, true, Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProduct {

        @Test
        @DisplayName("Should return 201 with Location header on success")
        void shouldReturn201() throws Exception {
            var response = sampleResponse();
            given(productUseCase.createProduct(any())).willReturn(response);

            var request = new CreateProductRequest(
                    "Notebook Pro", "High performance laptop", "NB-001",
                    new BigDecimal("4999.99"), "BRL", 10
            );

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/products/" + response.id()))
                    .andExpect(jsonPath("$.name").value("Notebook Pro"))
                    .andExpect(jsonPath("$.sku").value("NB-001"));
        }

        @Test
        @DisplayName("Should return 400 on validation failure")
        void shouldReturn400OnValidationError() throws Exception {
            var invalid = """
                {"name": "", "sku": "INVALID SKU WITH SPACES", "price": -10, "currency": "BRL", "initialStock": 0}
                """;

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isMap());
        }

        @Test
        @DisplayName("Should return 409 on duplicate SKU")
        void shouldReturn409OnDuplicateSku() throws Exception {
            given(productUseCase.createProduct(any()))
                    .willThrow(new DuplicateResourceException("Product", "SKU", "NB-001"));

            var request = new CreateProductRequest(
                    "Notebook Pro", "Desc", "NB-001",
                    new BigDecimal("4999.99"), "BRL", 10
            );

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class GetProducts {

        @Test
        @DisplayName("Should return 200 with product list")
        void shouldReturnProductList() throws Exception {
            given(productUseCase.findAll()).willReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].sku").value("NB-001"));
        }

        @Test
        @DisplayName("Should return 404 when product not found by ID")
        void shouldReturn404() throws Exception {
            var id = UUID.randomUUID();
            given(productUseCase.findById(id))
                    .willThrow(new ResourceNotFoundException("Product", id));

            mockMvc.perform(get("/api/v1/products/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProduct {

        @Test
        @DisplayName("Should return 204 on successful soft delete")
        void shouldReturn204() throws Exception {
            var id = UUID.randomUUID();
            willDoNothing().given(productUseCase).deleteProduct(id);

            mockMvc.perform(delete("/api/v1/products/{id}", id))
                    .andExpect(status().isNoContent());
        }
    }
}