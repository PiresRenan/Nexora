package com.nexora.application.usecase;

import com.nexora.application.dto.user.CreateUserRequest;
import com.nexora.application.dto.user.UpdateUserRequest;
import com.nexora.application.dto.user.UserResponse;
import com.nexora.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

/**
 * Input Port — operações de gerenciamento de usuários por staff (MANAGER/ADMIN).
 *
 * Auto-cadastro público usa {@link AuthUseCase#register}.
 */
public interface UserUseCase {

    /** Cria usuário com papel explícito — exclusivo para MANAGER/ADMIN. */
    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    UserResponse findById(UUID id);

    List<UserResponse> findAll();

    List<UserResponse> findAllByRole(UserRole role);

    UserResponse changeRole(UUID id, UserRole newRole);

    /** Desativa (soft delete) — usuário perde acesso mas histórico é preservado. */
    void deleteUser(UUID id);

    /** Reativa um usuário previamente desativado. */
    UserResponse activateUser(UUID id);
}