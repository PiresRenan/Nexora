package com.nexora.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuração da aplicação.
 * <p>
 * Melhoria: PasswordEncoder configurado desde o início.
 * Usamos spring-security-crypto (sem Spring Security completo) para
 * BCrypt — evita refatorações quando Spring Security for adicionado na Fase 3.
 */
@Configuration
public class ApplicationConfig {

    /**
     * BCrypt com strength=12 — equilíbrio entre segurança e performance.
     * Strength padrão é 10; 12 é mais seguro para dados de produção.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}