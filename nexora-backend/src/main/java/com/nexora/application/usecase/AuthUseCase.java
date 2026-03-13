package com.nexora.application.usecase;

import com.nexora.application.dto.auth.*;
import com.nexora.application.dto.user.UserResponse;

import java.util.UUID;

/**
 * Input Port — operações de autenticação e gestão de sessão.
 *
 * Rota de registro ({@link #register}) é pública e não exige autenticação.
 * Sempre cria usuários com papel CUSTOMER — papéis elevados exigem
 * criação via {@link UserUseCase#createUser} por um MANAGER/ADMIN.
 */
public interface AuthUseCase {

    /** Auto-cadastro público. Cria usuário CUSTOMER e retorna JWT imediato. */
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    /** Troca de senha pelo próprio usuário autenticado. */
    void changePassword(UUID userId, ChangePasswordRequest request);

    UserResponse me(UUID userId);
}