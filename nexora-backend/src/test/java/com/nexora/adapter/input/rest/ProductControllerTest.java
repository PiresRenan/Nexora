package com.nexora.adapter.input.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.application.dto.product.ProductResponse;
import com.nexora.application.usecase.ProductUseCase;
import com.nexora.domain.exception.DuplicateResourceException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.infrastructure.security.JwtAuthenticationFilter;
import com.nexora.infrastructure.security.JwtTokenProvider;
import com.nexora.infrastructure.security.NexoraUserDetailsService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test da camada web — carrega apenas ProductController + Security.
 * Usa @WithMockUser para simular usuário autenticado nas rotas protegidas.
 * Beans de infraestrutura (JwtTokenProvider, NexoraUserDetailsService) são
 * mockados para que @WebMvcTest não precise do contexto de persistência completo.
 */
@WebMvcTest(ProductController.class)
@DisplayName("ProductController")
class ProductControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ProductUseCase            productUseCase;
    // Mocks dos beans exigidos pelo SecurityConfig no contexto de slice
    @MockBean JwtTokenProvider          jwtTokenProvider;
    @MockBean NexoraUserDetailsService  userDetailsService;
    @MockBean JwtAuthenticationFilter   jwtAuthenticationFilter;

    private ProductResponse sampleResponse() {
        return new ProductResponse(
                UUID.fromString("b0000000-0000-0000-0000-000000000001"),
                "Notebook Pro", "High performance laptop", "NB-001",
                new BigDecimal("4999.99"), "BRL",
                10, null,          // stockQuantity, categoryId
                true, Instant.now(), Instant.now()
        );
    }

    // ─── POST /api/v1/products ─────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProduct {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 201 with Location header on success")
        void shouldReturn201() throws Exception {
            var response = sampleResponse();
            given(productUseCase.createProduct(any())).willReturn(response);

            var request = new CreateProductRequest(
                    "Notebook Pro", "High performance laptop", "NB-001",
                    new BigDecimal("4999.99"), "BRL", 10, null  // categoryId = null
            );

            mockMvc.perform(post("/api/v1/products").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/v1/products/" + response.id()))
                    .andExpect(jsonPath("$.name").value("Notebook Pro"))
                    .andExpect(jsonPath("$.sku").value("NB-001"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 on validation failure")
        void shouldReturn400OnValidationError() throws Exception {
            var invalid = """
                {"name": "", "sku": "INVALID SKU WITH SPACES", "price": -10, "currency": "BRL", "initialStock": 0}
                """;

            mockMvc.perform(post("/api/v1/products").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalid))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 409 on duplicate SKU")
        void shouldReturn409OnDuplicateSku() throws Exception {
            given(productUseCase.createProduct(any()))
                    .willThrow(new DuplicateResourceException("Product", "SKU", "NB-001"));

            var request = new CreateProductRequest(
                    "Notebook Pro", "Desc", "NB-001",
                    new BigDecimal("4999.99"), "BRL", 10, null
            );

            mockMvc.perform(post("/api/v1/products").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Unauthenticated request should return 403")
        void shouldReturn403WhenUnauthenticated() throws Exception {
            mockMvc.perform(post("/api/v1/products").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── GET /api/v1/products ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/products")
    class GetProducts {

        @Test
        @DisplayName("GET (public) should return 200 with paginated product list")
        void shouldReturnProductPage() throws Exception {
            var pageable  = PageRequest.of(0, 20);
            var page      = new PageImpl<>(List.of(sampleResponse()), pageable, 1);
            given(productUseCase.findAll(any())).willReturn(page);

            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].sku").value("NB-001"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("GET /{id} (public) should return 404 when product not found")
        void shouldReturn404() throws Exception {
            var id = UUID.randomUUID();
            given(productUseCase.findById(id))
                    .willThrow(new ResourceNotFoundException("Product", id));

            mockMvc.perform(get("/api/v1/products/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }
    }

    // ─── DELETE /api/v1/products/{id} ─────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/products/{id}")
    class DeleteProduct {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 204 on successful soft delete")
        void shouldReturn204() throws Exception {
            var id = UUID.randomUUID();
            willDoNothing().given(productUseCase).deleteProduct(id);

            mockMvc.perform(delete("/api/v1/products/{id}", id).with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }
}