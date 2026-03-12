package com.nexora.application.service;

import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.application.dto.product.UpdateProductRequest;
import com.nexora.domain.exception.DuplicateResourceException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.Money;
import com.nexora.domain.model.Product;
import com.nexora.domain.model.StockQuantity;
import com.nexora.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductApplicationService")
class ProductApplicationServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductApplicationService service;

    private Product existingProduct;

    @BeforeEach
    void setUp() {
        existingProduct = Product.create(
                "Test Product", "Description", "TST-001",
                Money.brl("100.00"), StockQuantity.of(20)
        );
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("Should create product successfully")
        void shouldCreateProduct() {
            var request = new CreateProductRequest(
                    "Test Product", "Description", "NEW-001",
                    new BigDecimal("100.00"), "BRL", 10
            );
            given(productRepository.existsBySku("NEW-001")).willReturn(false);
            given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            var result = service.createProduct(request);

            assertThat(result.name()).isEqualTo("Test Product");
            assertThat(result.sku()).isEqualTo("NEW-001");
            assertThat(result.stockQuantity()).isEqualTo(10);
            then(productRepository).should().save(any());
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException for existing SKU")
        void shouldThrowOnDuplicateSku() {
            var request = new CreateProductRequest(
                    "Name", "Desc", "EXISTING-001",
                    BigDecimal.TEN, "BRL", 5
            );
            given(productRepository.existsBySku("EXISTING-001")).willReturn(true);

            assertThatThrownBy(() -> service.createProduct(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("EXISTING-001");

            then(productRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return product when found")
        void shouldReturnProduct() {
            given(productRepository.findById(existingProduct.getId()))
                    .willReturn(Optional.of(existingProduct));

            var result = service.findById(existingProduct.getId());

            assertThat(result.id()).isEqualTo(existingProduct.getId());
            assertThat(result.name()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            var randomId = UUID.randomUUID();
            given(productRepository.findById(randomId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(randomId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(randomId.toString());
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Should return all products mapped to responses")
        void shouldReturnAllProducts() {
            given(productRepository.findAll()).willReturn(List.of(existingProduct));

            var result = service.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().sku()).isEqualTo("TST-001");
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("Should deactivate (soft delete) product")
        void shouldDeactivateProduct() {
            given(productRepository.findById(existingProduct.getId()))
                    .willReturn(Optional.of(existingProduct));
            given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.deleteProduct(existingProduct.getId());

            // Verifica que o produto foi salvo como inativo
            then(productRepository).should().save(argThat(p -> !p.isActive()));
        }
    }
}