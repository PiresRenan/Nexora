package com.nexora.infrastructure.messaging;

import com.nexora.domain.event.DomainEvent;
import com.nexora.domain.port.EventPublisher;
import com.nexora.infrastructure.config.NexoraProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementação do EventPublisher via Kafka.
 * Condicionado à existência do KafkaTemplate — garante que o bean não é criado
 * quando Kafka está desabilitado em testes.
 *
 * Roteamento de tópicos:
 *   order.*  → nexora.orders
 *   stock.*  → nexora.stock
 *   user.*   → nexora.users
 *
 * A chave da mensagem = aggregateId — garante ordenação por entidade na mesma partição.
 */
@Component
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final NexoraProperties              props;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                               NexoraProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props         = props;
    }

    @Override
    public void publish(DomainEvent event) {
        String topic = resolveTopic(event.eventType());
        String key   = event.aggregateId().toString();

        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event type={} aggregate={} topic={}: {}",
                                event.eventType(), event.aggregateId(), topic, ex.getMessage());
                    } else {
                        log.debug("Published event type={} aggregate={} → topic={} partition={}",
                                event.eventType(), event.aggregateId(), topic,
                                result.getRecordMetadata().partition());
                    }
                });
    }

    private String resolveTopic(String eventType) {
        if (eventType.startsWith("order.")) return props.kafka().orders();
        if (eventType.startsWith("stock.")) return props.kafka().stock();
        if (eventType.startsWith("user."))  return props.kafka().users();
        log.warn("Unknown event type '{}' — routing to orders topic", eventType);
        return props.kafka().orders();
    }
}