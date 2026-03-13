package com.nexora.domain.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID    eventId,
        Instant occurredAt,
        UUID    orderId,
        UUID    customerId,
        UUID    cancelledBy,
        String  reason
) implements DomainEvent {

    @Override public String eventType()    { return "order.cancelled"; }
    @Override public String aggregateType(){ return "Order"; }
    @Override public UUID   aggregateId()  { return orderId; }

    public static OrderCancelledEvent of(UUID orderId, UUID customerId,
                                         UUID cancelledBy, String reason) {
        return new OrderCancelledEvent(UUID.randomUUID(), Instant.now(),
                orderId, customerId, cancelledBy, reason);
    }
}