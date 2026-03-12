package com.nexora.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money Value Object")
class MoneyTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Should create Money with valid values")
        void shouldCreateWithValidValues() {
            var money = Money.brl("100.00");
            assertThat(money.amount()).isEqualByComparingTo("100.00");
            assertThat(money.currency()).isEqualTo("BRL");
        }

        @Test
        @DisplayName("Should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> Money.brl("-1.00"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("Should reject null currency")
        void shouldRejectNullCurrency() {
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should normalize scale to 2 decimal places")
        void shouldNormalizeScale() {
            var money = Money.brl("100");
            assertThat(money.amount().scale()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Operations")
    class Operations {

        @Test
        @DisplayName("Should add two Money values with same currency")
        void shouldAddSameCurrency() {
            var a = Money.brl("100.00");
            var b = Money.brl("50.00");
            var result = a.add(b);
            assertThat(result.amount()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("Should not add Money with different currencies")
        void shouldNotAddDifferentCurrencies() {
            var brl = Money.brl("100.00");
            var usd = Money.of(BigDecimal.TEN, "USD");
            assertThatThrownBy(() -> brl.add(usd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different currencies");
        }

        @Test
        @DisplayName("Should subtract Money values")
        void shouldSubtract() {
            var a = Money.brl("100.00");
            var b = Money.brl("30.00");
            assertThat(a.subtract(b).amount()).isEqualByComparingTo("70.00");
        }

        @Test
        @DisplayName("Should not subtract resulting in negative")
        void shouldNotSubtractNegative() {
            var a = Money.brl("10.00");
            var b = Money.brl("50.00");
            assertThatThrownBy(() -> a.subtract(b))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negative");
        }
    }
}