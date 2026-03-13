package com.nexora.infrastructure.config;

import com.nexora.domain.port.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fornece uma implementação no-op de EventPublisher como fallback.
 * Usada quando o KafkaEventPublisher não está disponível (ex: testes sem Kafka).
 *
 * O @ConditionalOnMissingBean garante que o KafkaEventPublisher, quando presente,
 * tem precedência — sem qualquer conflito de bean.
 */
@Configuration
public class EventPublisherConfig {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherConfig.class);

    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher noOpEventPublisher() {
        log.warn("No EventPublisher configured — using no-op. Events will NOT be published.");
        return event -> log.debug("[NO-OP] Event dropped: type={} aggregate={}",
                event.eventType(), event.aggregateId());
    }
}