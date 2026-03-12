package com.nexora.domain.model;

/**
 * Value Object para quantidade em estoque.
 * Garante que o estoque nunca seja negativo — invariante de domínio.
 */
public record StockQuantity(int value) {

    public StockQuantity {
        if (value < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative: " + value);
        }
    }

    public static StockQuantity of(int value) {
        return new StockQuantity(value);
    }

    public static StockQuantity zero() {
        return new StockQuantity(0);
    }

    public StockQuantity add(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive: " + quantity);
        }
        return new StockQuantity(this.value + quantity);
    }

    public StockQuantity subtract(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to remove must be positive: " + quantity);
        }
        if (this.value < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: %d, requested: %d".formatted(this.value, quantity));
        }
        return new StockQuantity(this.value - quantity);
    }

    public boolean isAvailableFor(int requested) {
        return this.value >= requested;
    }

    public boolean isEmpty() {
        return this.value == 0;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}