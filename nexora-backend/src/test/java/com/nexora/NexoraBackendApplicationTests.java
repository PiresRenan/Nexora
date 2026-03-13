package com.nexora;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — garante que o ApplicationContext Spring sobe sem erros.
 *
 * Usa H2 em memória + Flyway desabilitado + Kafka/Redis desabilitados
 * (configurados via src/test/resources/application.yml).
 * Hibernate cria o schema via create-drop para o H2.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Spring context loads successfully")
class NexoraApplicationTests {

    @Test
    @DisplayName("Application context deve subir sem erros de configuração")
    void contextLoads() {
        // Se este teste passar, todos os beans foram criados com sucesso:
        //  - SecurityConfig (JwtFilter, AuthManager, etc.)
        //  - EventPublisherConfig (no-op fallback ativo — sem Kafka)
        //  - CacheConfig desabilitado (spring.cache.type=none)
        //  - Todos os Application Services com injeção de dependência válida
    }
}