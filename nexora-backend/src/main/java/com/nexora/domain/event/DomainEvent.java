package com.nexora.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato base para todos os eventos de domínio do Nexora.
 * Eventos são fatos imutáveis que descrevem algo que aconteceu.
 */
public interface DomainEvent {
    UUID    eventId();       // UUID único — usado para idempotência pelo consumer
    String  eventType();     // ex: "order.confirmed"
    String  aggregateType(); // ex: "Order"
    UUID    aggregateId();   // ID do aggregate root do evento
    Instant occurredAt();
}