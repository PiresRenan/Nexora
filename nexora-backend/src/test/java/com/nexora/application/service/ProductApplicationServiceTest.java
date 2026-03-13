package com.nexora.application.service;

import com.nexora.application.dto.product.CreateProductRequest;
import com.nexora.domain.exception.DuplicateResourceException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.*;
import com.nexora.domain.port.EventPublisher;
import com.nexora.domain.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductApplicationService")
class ProductApplicationServiceTest {

    @Mock ProductRepository       productRepository;
    @Mock CategoryRepository      categoryRepository;     // Phase 2: necessário no construtor
    @Mock StockMovementRepository stockMovementRepository; // necessário no construtor
    @Mock EventPublisher          eventPublisher;          // Phase 3: necessário no construtor

    @InjectMocks ProductApplicationService service;

    private Product existingProduct;

    @BeforeEach
    void setUp() {
        existingProduct = Product.create(
                "Test Product", "Description", "TST-001",
                Money.brl("100.00"), StockQuantity.of(20)
        );
    }

    // ─── createProduct ────────────────────────────────────────────────────

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("Should create product successfully")
        void shouldCreateProduct() {
            // categoryId = null (opcional)
            var request = new CreateProductRequest(
                    "Test Product", "Description", "NEW-001",
                    new BigDecimal("100.00"), "BRL", 10, null
            );
            given(productRepository.existsBySku("NEW-001")).willReturn(false);
            given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(stockMovementRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            var result = service.createProduct(request);

            assertThat(result.name()).isEqualTo("Test Product");
            assertThat(result.sku()).isEqualTo("NEW-001");
            assertThat(result.stockQuantity()).isEqualTo(10);
            then(productRepository).should().save(any());
            then(stockMovementRepository).should().save(any()); // movimento INITIAL_STOCK
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException for existing SKU")
        void shouldThrowOnDuplicateSku() {
            var request = new CreateProductRequest(
                    "Name", "Desc", "EXISTING-001", BigDecimal.TEN, "BRL", 5, null
            );
            given(productRepository.existsBySku("EXISTING-001")).willReturn(true);

            assertThatThrownBy(() -> service.createProduct(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("EXISTING-001");

            then(productRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for invalid category")
        void shouldThrowOnInvalidCategory() {
            var catId = UUID.randomUUID();
            var request = new CreateProductRequest(
                    "Name", "Desc", "CAT-001", BigDecimal.TEN, "BRL", 5, catId
            );
            given(productRepository.existsBySku("CAT-001")).willReturn(false);
            given(categoryRepository.findById(catId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createProduct(request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── findById ─────────────────────────────────────────────────────────

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

    // ─── findAll ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll (paginated)")
    class FindAll {

        @Test
        @DisplayName("Should return paginated products")
        void shouldReturnPagedProducts() {
            var pageable = PageRequest.of(0, 20);
            var page = new PageImpl<>(List.of(existingProduct), pageable, 1);
            given(productRepository.findAll(pageable)).willReturn(page);

            var result = service.findAll(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().sku()).isEqualTo("TST-001");
        }
    }

    // ─── deleteProduct ────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("Should deactivate product (soft delete)")
        void shouldDeactivateProduct() {
            given(productRepository.findById(existingProduct.getId()))
                    .willReturn(Optional.of(existingProduct));
            given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            service.deleteProduct(existingProduct.getId());

            then(productRepository).should().save(argThat(p -> !p.isActive()));
        }
    }

    // ─── replenishStock ───────────────────────────────────────────────────

    @Nested
    @DisplayName("replenishStock")
    class ReplenishStock {

        @Test
        @DisplayName("Should increment stock and publish event")
        void shouldReplenishAndPublishEvent() {
            var performedBy = UUID.randomUUID();
            given(productRepository.findById(existingProduct.getId()))
                    .willReturn(Optional.of(existingProduct));
            given(productRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(stockMovementRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            willDoNothing().given(eventPublisher).publish(any());

            var result = service.replenishStock(existingProduct.getId(), 5, performedBy);

            assertThat(result.stockQuantity()).isEqualTo(25); // 20 + 5
            then(stockMovementRepository).should().save(argThat(m -> m.getType() == StockMovement.Type.ENTRY));
            then(eventPublisher).should().publish(any());
        }
    }
}