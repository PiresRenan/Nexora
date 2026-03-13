package com.nexora.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades customizadas do Nexora (topics Kafka, features flags, etc.).
 */
@ConfigurationProperties(prefix = "nexora")
public record NexoraProperties(
        KafkaTopics kafka
) {
    public record KafkaTopics(String orders, String stock, String users) {}
}