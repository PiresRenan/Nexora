package com.nexora.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderStatus State Machine")
class OrderStatusTest {

    @ParameterizedTest(name = "{0} → {1} should be allowed")
    @CsvSource({"PENDING,CONFIRMED", "PENDING,CANCELLED",
            "CONFIRMED,SHIPPED", "CONFIRMED,CANCELLED",
            "SHIPPED,DELIVERED"})
    void allowedTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} → {1} should be forbidden")
    @CsvSource({"DELIVERED,CANCELLED", "CANCELLED,CONFIRMED",
            "PENDING,DELIVERED",   "SHIPPED,PENDING"})
    void forbiddenTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test @DisplayName("DELIVERED and CANCELLED are final states")
    void finalStates() {
        assertThat(OrderStatus.DELIVERED.isFinal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isFinal()).isTrue();
        assertThat(OrderStatus.PENDING.isFinal()).isFalse();
    }
}