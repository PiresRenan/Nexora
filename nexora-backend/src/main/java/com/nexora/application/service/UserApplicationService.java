package com.nexora.application.service;

import com.nexora.application.dto.user.*;
import com.nexora.application.usecase.UserUseCase;
import com.nexora.domain.event.UserRegisteredEvent;
import com.nexora.domain.exception.*;
import com.nexora.domain.model.*;
import com.nexora.domain.port.EventPublisher;
import com.nexora.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserApplicationService implements UserUseCase {

    private static final Logger log = LoggerFactory.getLogger(UserApplicationService.class);

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher  eventPublisher;

    public UserApplicationService(UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  EventPublisher eventPublisher) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher  = eventPublisher;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating user email={}", request.email());
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }
        var user  = User.create(request.name(), request.email(),
                passwordEncoder.encode(request.password()), request.role());
        var saved = userRepository.save(user);

        eventPublisher.publish(UserRegisteredEvent.of(saved.getId(), saved.getEmail(), saved.getRole()));

        log.info("User created id={}", saved.getId());
        return UserResponse.fromDomain(saved);
    }

    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        var user = findOrThrow(id);
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }
        user.updateProfile(request.name(), request.email());
        return UserResponse.fromDomain(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return UserResponse.fromDomain(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::fromDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAllByRole(UserRole role) {
        return userRepository.findAllByRole(role).stream().map(UserResponse::fromDomain).toList();
    }

    @Override
    public UserResponse changeRole(UUID id, UserRole newRole) {
        var user = findOrThrow(id);
        user.changeRole(newRole);
        return UserResponse.fromDomain(userRepository.save(user));
    }

    @Override
    public void deleteUser(UUID id) {
        var user = findOrThrow(id);
        user.deactivate();
        userRepository.save(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}