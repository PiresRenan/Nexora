package com.nexora.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Cria os tópicos Kafka automaticamente se não existirem.
 * Apenas ativo quando spring.kafka.bootstrap-servers está configurado.
 */
@Configuration
@ConditionalOnProperty("spring.kafka.bootstrap-servers")
public class KafkaConfig {

    private final NexoraProperties props;

    public KafkaConfig(NexoraProperties props) {
        this.props = props;
    }

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(props.kafka().orders())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic stockTopic() {
        return TopicBuilder.name(props.kafka().stock())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic usersTopic() {
        return TopicBuilder.name(props.kafka().users())
                .partitions(1)
                .replicas(1)
                .build();
    }
}