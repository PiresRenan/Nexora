package com.nexora.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI nexoraOpenAPI() {
        var bearerScheme = new SecurityScheme()
                .name(SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Access Token. Obtenha via POST /api/v1/auth/login");

        return new OpenAPI()
                .info(new Info()
                        .title("Nexora Store Management API")
                        .description("""
                    Backend do sistema Nexora — gestão de loja com Hexagonal Architecture.
                    Desenvolvido por **Renan Pires** como projeto de portfólio.
                    
                    ## Autenticação
                    Use **POST /api/v1/auth/login** para obter o JWT, depois clique em **Authorize ↗**.
                    
                    ## Fase 3 (atual)
                    - **Cache Redis**: produtos e categorias são cacheados automaticamente
                    - **Kafka**: eventos de domínio publicados em `nexora.orders`, `nexora.stock`, `nexora.users`
                    """)
                        .version("v3.0.0")
                        .contact(new Contact().name("Renan Pires").url("https://github.com/renan-pires"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SCHEME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }
}