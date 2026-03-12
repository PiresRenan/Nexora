package com.nexora.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração do OpenAPI 3.0 via Springdoc.
 * Melhoria: documentação automática disponível desde a Fase 1.
 * Acesso: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nexoraOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Nexora Store Management API")
                        .description("""
                    Backend principal do sistema Nexora — ecossistema de gestão de loja.
                    Desenvolvido por Renan Pires como projeto de portfólio de engenharia de software.
                    """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Renan Pires")
                                .url("https://github.com/renan-pires"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}