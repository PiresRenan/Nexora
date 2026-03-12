package com.nexora.application.dto.auth;

import com.nexora.domain.model.UserRole;

import java.util.UUID;

public record AuthResponse(
        String  accessToken,
        String  refreshToken,
        String  tokenType,
        long    expiresIn,      // segundos
        UUID    userId,
        String  email,
        UserRole role
) {
    public static AuthResponse of(
            String accessToken, String refreshToken,
            long accessTokenExpMs, UUID userId, String email, UserRole role
    ) {
        return new AuthResponse(
                accessToken, refreshToken, "Bearer",
                accessTokenExpMs / 1000,
                userId, email, role
        );
    }
}