package com.nexora.application.dto.user;

import com.nexora.domain.model.User;
import com.nexora.domain.model.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta de usuário.
 * Nunca expõe o passwordHash — segurança por design.
 */
public record UserResponse(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse fromDomain(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}