package com.nexora.security;

import com.nexora.infrastructure.security.JwtProperties;
import com.nexora.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String SECRET =
            "nexora-test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256";

    @BeforeEach
    void setUp() {
        var props = new JwtProperties(SECRET, 900_000L, 604_800_000L);
        tokenProvider = new JwtTokenProvider(props);
    }

    @Test
    @DisplayName("Should generate valid access token and extract claims")
    void shouldGenerateAndExtractAccessToken() {
        var userId = UUID.randomUUID();
        var token  = tokenProvider.generateAccessToken(userId, "user@test.com", "ADMIN");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.extractUserId(token)).isEqualTo(userId);
        assertThat(tokenProvider.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateRefreshToken() {
        var userId = UUID.randomUUID();
        var token  = tokenProvider.generateRefreshToken(userId);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should reject tampered token")
    void shouldRejectTamperedToken() {
        var token = tokenProvider.generateAccessToken(UUID.randomUUID(), "user@test.com", "ADMIN");
        assertThat(tokenProvider.validateToken(token + "tampered")).isFalse();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Token com expiração 1ms (já expirado ao validar)
        var props = new JwtProperties(SECRET, 1L, 1L);
        var expiredProvider = new JwtTokenProvider(props);
        var token = expiredProvider.generateAccessToken(UUID.randomUUID(), "u@t.com", "CUSTOMER");

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        assertThat(expiredProvider.validateToken(token)).isFalse();
    }
}