package com.nexora.application.dto.order;

import com.nexora.domain.model.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID       productId,
        String     productName,
        String     productSku,
        BigDecimal unitPrice,
        String     currency,
        int        quantity,
        BigDecimal subtotal
) {
    public static OrderItemResponse fromDomain(OrderItem item) {
        return new OrderItemResponse(
                item.productId(),
                item.productName(),
                item.productSku(),
                item.unitPrice().amount(),
                item.unitPrice().currency(),
                item.quantity(),
                item.subtotal().amount()
        );
    }
}