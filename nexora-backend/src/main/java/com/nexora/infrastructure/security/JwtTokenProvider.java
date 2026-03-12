package com.nexora.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Responsável por criar e validar tokens JWT.
 *
 * Access Token: vida curta (15 min) — carrega claims de autorização.
 * Refresh Token: vida longa (7 dias) — só carrega o subject (userId).
 *
 * Melhoria de segurança:
 * - jti (JWT ID) único em cada token para futuro suporte a revogação
 * - exp rigoroso validado na leitura
 * - Secrets derivados de HMAC-SHA256 (mínimo 256 bits)
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long accessTokenExpMs;
    private final long refreshTokenExpMs;

    public JwtTokenProvider(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpMs  = props.accessTokenExpirationMs();
        this.refreshTokenExpMs = props.refreshTokenExpirationMs();
    }

    // ─── Token Generation ──────────────────────────────────────────────────

    public String generateAccessToken(UUID userId, String email, String role) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claims(Map.of("email", email, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpMs))
                .signWith(key)
                .compact();
    }

    // ─── Token Parsing ─────────────────────────────────────────────────────

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public String extractRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        }
        return false;
    }

    public long getRefreshTokenExpMs() {
        return refreshTokenExpMs;
    }
}