package com.nexora.application.usecase;

import com.nexora.application.dto.user.CreateUserRequest;
import com.nexora.application.dto.user.UpdateUserRequest;
import com.nexora.application.dto.user.UserResponse;
import com.nexora.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

/**
 * Input Port — define as operações disponíveis para usuários.
 */
public interface UserUseCase {

    UserResponse createUser(CreateUserRequest request);

    UserResponse updateUser(UUID id, UpdateUserRequest request);

    UserResponse findById(UUID id);

    List<UserResponse> findAll();

    List<UserResponse> findAllByRole(UserRole role);

    UserResponse changeRole(UUID id, UserRole newRole);

    void deleteUser(UUID id);
}