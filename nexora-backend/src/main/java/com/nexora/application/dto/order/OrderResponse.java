package com.nexora.application.dto.order;

import com.nexora.domain.model.Order;
import com.nexora.domain.model.OrderItem;
import com.nexora.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID             id,
        UUID             customerId,
        List<OrderItemResponse> items,
        OrderStatus      status,
        BigDecimal       total,
        String           currency,
        String           notes,
        Instant          createdAt,
        Instant          updatedAt
) {
    public static OrderResponse fromDomain(Order order) {
        var items = order.getItems().stream()
                .map(OrderItemResponse::fromDomain)
                .toList();

        var total = order.getItems().isEmpty() ? null : order.calculateTotal();

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                items,
                order.getStatus(),
                total != null ? total.amount() : BigDecimal.ZERO,
                total != null ? total.currency() : "BRL",
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}