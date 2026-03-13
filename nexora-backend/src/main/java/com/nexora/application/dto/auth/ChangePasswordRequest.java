package com.nexora.application.dto.auth;

import jakarta.validation.constraints.*;

/**
 * DTO para troca de senha pelo próprio usuário.
 * Exige a senha atual para prevenir account takeover
 * caso o token seja roubado com sessão ativa.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
        String newPassword
) {}