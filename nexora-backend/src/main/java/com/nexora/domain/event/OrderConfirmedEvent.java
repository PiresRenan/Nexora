package com.nexora.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Disparado quando um pedido é confirmado e o estoque foi decrementado.
 * Carrega snapshot completo do pedido para consumers downstream.
 */
public record OrderConfirmedEvent(
        UUID    eventId,
        Instant occurredAt,
        UUID    orderId,
        UUID    customerId,
        BigDecimal total,
        String  currency,
        List<ItemSnapshot> items
) implements DomainEvent {

    public record ItemSnapshot(UUID productId, String sku, int quantity, BigDecimal unitPrice) {}

    @Override public String eventType()    { return "order.confirmed"; }
    @Override public String aggregateType(){ return "Order"; }
    @Override public UUID   aggregateId()  { return orderId; }

    public static OrderConfirmedEvent of(UUID orderId, UUID customerId,
                                         BigDecimal total, String currency,
                                         List<ItemSnapshot> items) {
        return new OrderConfirmedEvent(UUID.randomUUID(), Instant.now(),
                orderId, customerId, total, currency, items);
    }
}