package com.nexora.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades JWT carregadas do application.yml.
 * Uso de @ConfigurationProperties (record) para type-safe config.
 */
@ConfigurationProperties(prefix = "nexora.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs
) {}