package com.nexora.application.dto.auth;

import jakarta.validation.constraints.*;

/**
 * DTO para auto-cadastro público.
 *
 * Não expõe o campo {@code role} — usuários criados por esta rota recebem
 * {@link com.nexora.domain.model.UserRole#CUSTOMER} automaticamente.
 *
 * Para criar funcionários com papéis elevados, um MANAGER/ADMIN deve usar
 * POST /api/v1/users com o campo {@code role} explícito.
 */
public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {}