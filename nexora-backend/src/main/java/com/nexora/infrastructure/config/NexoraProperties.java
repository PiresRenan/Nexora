package com.nexora.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades customizadas do Nexora carregadas do application.yml.
 * Centraliza: JWT, tópicos Kafka e configuração de storage.
 */
@ConfigurationProperties(prefix = "nexora")
public record NexoraProperties(KafkaTopics kafka) {

    public record KafkaTopics(String orders, String stock, String users) {}
}