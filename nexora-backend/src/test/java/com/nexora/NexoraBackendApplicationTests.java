package com.nexora;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — ApplicationContext sobe sem erros com toda a Fase 4.
 *
 * Condições ativas:
 *  - H2 em memória (Flyway desabilitado, Hibernate cria via create-drop)
 *  - Kafka/Redis desabilitados via autoconfigure.exclude
 *  - MinIO: nexora.storage.endpoint AUSENTE → MinioConfig não criado
 *    → MinioStorageAdapter não criado → StorageAdapterConfig cria no-op
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Spring context loads with all Phase 4 beans")
class NexoraApplicationTests {

    @Test
    @DisplayName("Context must start without errors")
    void contextLoads() {
        // Valida: todos os beans criados, sem dependências circulares,
        // StoragePort no-op ativo, EventPublisher no-op ativo
    }
}