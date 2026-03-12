package com.nexora.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Linha de pedido — Value Object dentro do aggregate Order.
 * Captura snapshot do preço no momento da compra.
 * Imutável: uma vez criado, não muda.
 */
public record OrderItem(
        UUID productId,
        String productName,
        String productSku,
        Money unitPrice,        // snapshot do preço no momento do pedido
        int quantity
) {

    public OrderItem {
        Objects.requireNonNull(productId,   "Product ID cannot be null");
        Objects.requireNonNull(productName, "Product name cannot be null");
        Objects.requireNonNull(productSku,  "Product SKU cannot be null");
        Objects.requireNonNull(unitPrice,   "Unit price cannot be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
        }
    }

    public static OrderItem of(Product product, int quantity) {
        return new OrderItem(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getPrice(),
                quantity
        );
    }

    /** Valor total desta linha = preço unitário × quantidade */
    public Money subtotal() {
        var total = unitPrice.amount().multiply(java.math.BigDecimal.valueOf(quantity));
        return Money.of(total, unitPrice.currency());
    }
}