package com.nexora.domain.event;

import java.time.Instant;
import java.util.UUID;

public record StockReplenishedEvent(
        UUID    eventId,
        Instant occurredAt,
        UUID    productId,
        String  productSku,
        int     quantity,
        int     stockAfter,
        String  reason,
        UUID    performedBy
) implements DomainEvent {

    @Override public String eventType()    { return "stock.replenished"; }
    @Override public String aggregateType(){ return "Product"; }
    @Override public UUID   aggregateId()  { return productId; }

    public static StockReplenishedEvent of(UUID productId, String sku, int qty,
                                           int stockAfter, String reason, UUID performedBy) {
        return new StockReplenishedEvent(UUID.randomUUID(), Instant.now(),
                productId, sku, qty, stockAfter, reason, performedBy);
    }
}