package com.nexora.domain.port;

import com.nexora.domain.event.DomainEvent;

/**
 * Output Port — contrato para publicação de eventos de domínio.
 * A implementação concreta (Kafka) está na infraestrutura.
 * Uma implementação no-op é usada em testes onde Kafka não está disponível.
 */
public interface EventPublisher {
    void publish(DomainEvent event);
}