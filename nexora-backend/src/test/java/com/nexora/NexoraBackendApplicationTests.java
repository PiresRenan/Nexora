package com.nexora;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — garante que o ApplicationContext Spring sobe sem erros.
 * Usa H2 em memória + Kafka/Redis desabilitados (via application.yml de teste).
 * Flyway desabilitado — Hibernate cria o schema via create-drop.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Spring Context loads successfully")
class NexoraApplicationTests {

    @Test
    @DisplayName("Application context should start without errors")
    void contextLoads() {
        // Se o contexto subir, o teste passa.
        // Valida: beans configurados corretamente, sem dependências circulares,
        // sem erros de binding de properties, sem conflitos de beans.
    }
}