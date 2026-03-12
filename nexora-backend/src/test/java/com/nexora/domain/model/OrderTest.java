package com.nexora.domain.model;

import com.nexora.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Order Aggregate")
class OrderTest {

    private static Product product;

    @BeforeAll
    static void setupProduct() {
        product = Product.create("Notebook", "desc", "NB-001",
                Money.brl("1000.00"), StockQuantity.of(10));
    }

    @Test
    @DisplayName("Should create order and add items")
    void shouldCreateAndAddItems() {
        var order = Order.create(UUID.randomUUID(), "test order");
        order.addItem(product, 2);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().getFirst().quantity()).isEqualTo(2);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Should consolidate repeated product into single item")
    void shouldConsolidateItems() {
        var order = Order.create(UUID.randomUUID(), null);
        order.addItem(product, 2);
        order.addItem(product, 3);

        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getItems().getFirst().quantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should calculate correct total")
    void shouldCalculateTotal() {
        var order = Order.create(UUID.randomUUID(), null);
        order.addItem(product, 3);
        var total = order.calculateTotal();

        assertThat(total.amount()).isEqualByComparingTo("3000.00");
        assertThat(total.currency()).isEqualTo("BRL");
    }

    @Test
    @DisplayName("Should confirm order and transition status")
    void shouldConfirmOrder() {
        var order = Order.create(UUID.randomUUID(), null);
        order.addItem(product, 1);
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should not cancel a DELIVERED order")
    void shouldNotCancelDelivered() {
        var order = Order.create(UUID.randomUUID(), null);
        order.addItem(product, 1);
        order.confirm();
        order.ship();
        order.deliver();

        assertThatThrownBy(() -> order.cancel("test"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DELIVERED");
    }

    @Test
    @DisplayName("Should not add items to non-PENDING order")
    void shouldNotAddItemsToConfirmedOrder() {
        var order = Order.create(UUID.randomUUID(), null);
        order.addItem(product, 1);
        order.confirm();

        assertThatThrownBy(() -> order.addItem(product, 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CONFIRMED");
    }

    @Test
    @DisplayName("Should not add item when product has insufficient stock")
    void shouldNotAddItemWithInsufficientStock() {
        var lowStockProduct = Product.create("Low", "desc", "LOW-001",
                Money.brl("10.00"), StockQuantity.of(2));
        var order = Order.create(UUID.randomUUID(), null);

        assertThatThrownBy(() -> order.addItem(lowStockProduct, 5))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not available");
    }
}