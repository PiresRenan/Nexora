package com.nexora.application.service;

import com.nexora.application.dto.user.CreateUserRequest;
import com.nexora.application.dto.user.UpdateUserRequest;
import com.nexora.application.dto.user.UserResponse;
import com.nexora.application.usecase.UserUseCase;
import com.nexora.domain.exception.DuplicateResourceException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.User;
import com.nexora.domain.model.UserRole;
import com.nexora.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementação dos casos de uso de usuário.
 * Melhoria: uso de PasswordEncoder (Spring Security Crypto, sem Spring Security completo)
 * para hash de senha desde o início — nunca armazenamos senha em texto plano.
 */
@Service
@Transactional
public class UserApplicationService implements UserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserApplicationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.email());

        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        var user = User.create(
                request.name(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.role()
        );

        var saved = userRepository.save(user);
        log.info("User created successfully: {}", saved.getId());
        return UserResponse.fromDomain(saved);
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("Updating user: {}", id);
        var user = findUserOrThrow(id);

        // Valida conflito de email apenas se o email está sendo alterado
        if (!user.getEmail().equals(request.email().toLowerCase())
                && userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        user.updateProfile(request.name(), request.email());
        return UserResponse.fromDomain(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return UserResponse.fromDomain(findUserOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAllByRole(UserRole role) {
        return userRepository.findAllByRole(role).stream()
                .map(UserResponse::fromDomain)
                .toList();
    }

    @Override
    public UserResponse changeRole(UUID id, UserRole newRole) {
        log.info("Changing role for user {} to {}", id, newRole);
        var user = findUserOrThrow(id);
        user.changeRole(newRole);
        return UserResponse.fromDomain(userRepository.save(user));
    }

    @Override
    public void deleteUser(UUID id) {
        log.info("Deactivating user: {}", id);
        var user = findUserOrThrow(id);
        user.deactivate();
        userRepository.save(user);
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private User findUserOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}