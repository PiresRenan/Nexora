package com.nexora.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Product Domain Entity")
class ProductTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.create(
                "Notebook Pro",
                "High performance laptop",
                "NB-001",
                Money.brl("4999.99"),
                StockQuantity.of(10)
        );
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Should create product with valid data")
        void shouldCreateWithValidData() {
            assertThat(product.getId()).isNotNull();
            assertThat(product.getName()).isEqualTo("Notebook Pro");
            assertThat(product.getSku()).isEqualTo("NB-001");
            assertThat(product.isActive()).isTrue();
            assertThat(product.getStock().value()).isEqualTo(10);
            assertThat(product.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not create product with blank name")
        void shouldNotCreateWithBlankName() {
            assertThatThrownBy(() -> Product.create(
                    "", "desc", "SKU-001",
                    Money.brl("10.00"), StockQuantity.zero()
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("Should not create product with blank SKU")
        void shouldNotCreateWithBlankSku() {
            assertThatThrownBy(() -> Product.create(
                    "Name", "desc", "  ",
                    Money.brl("10.00"), StockQuantity.zero()
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Stock Management")
    class StockManagement {

        @Test
        @DisplayName("Should replenish stock correctly")
        void shouldReplenishStock() {
            product.replenishStock(5);
            assertThat(product.getStock().value()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should withdraw stock correctly")
        void shouldWithdrawStock() {
            product.withdrawStock(3);
            assertThat(product.getStock().value()).isEqualTo(7);
        }

        @Test
        @DisplayName("Should not withdraw more than available stock")
        void shouldNotWithdrawMoreThanAvailable() {
            assertThatThrownBy(() -> product.withdrawStock(20))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient");
        }

        @Test
        @DisplayName("Should correctly check availability")
        void shouldCheckAvailability() {
            assertThat(product.isAvailable(5)).isTrue();
            assertThat(product.isAvailable(10)).isTrue();
            assertThat(product.isAvailable(11)).isFalse();
        }

        @Test
        @DisplayName("Inactive product should not be available")
        void inactiveShouldNotBeAvailable() {
            product.deactivate();
            assertThat(product.isAvailable(1)).isFalse();
        }
    }

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("Should deactivate product")
        void shouldDeactivate() {
            product.deactivate();
            assertThat(product.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should reactivate product")
        void shouldActivate() {
            product.deactivate();
            product.activate();
            assertThat(product.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should update updatedAt when modifying")
        void shouldUpdateTimestamp() throws InterruptedException {
            var before = product.getUpdatedAt();
            Thread.sleep(10);
            product.deactivate();
            assertThat(product.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }
}