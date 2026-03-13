package com.nexora.application.service;

import com.nexora.application.dto.auth.*;
import com.nexora.application.dto.user.UserResponse;
import com.nexora.application.usecase.AuthUseCase;
import com.nexora.domain.event.UserRegisteredEvent;
import com.nexora.domain.exception.BusinessRuleException;
import com.nexora.domain.exception.DuplicateResourceException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.User;
import com.nexora.domain.model.UserRole;
import com.nexora.domain.port.EventPublisher;
import com.nexora.domain.repository.UserRepository;
import com.nexora.infrastructure.security.JwtProperties;
import com.nexora.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso de autenticação e gestão de sessão.
 *
 * Responsabilidades:
 *  - Auto-cadastro público (papel fixo CUSTOMER + JWT imediato)
 *  - Login com validação de credenciais via AuthenticationManager
 *  - Renovação de access token via refresh token
 *  - Troca de senha autenticada (exige senha atual)
 *  - Dados do perfil do usuário logado
 */
@Service
@Transactional(readOnly = true)
public class AuthApplicationService implements AuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final AuthenticationManager authManager;
    private final JwtTokenProvider      tokenProvider;
    private final JwtProperties         jwtProperties;
    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final EventPublisher        eventPublisher;

    public AuthApplicationService(AuthenticationManager authManager,
                                  JwtTokenProvider tokenProvider,
                                  JwtProperties jwtProperties,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder,
                                  EventPublisher eventPublisher) {
        this.authManager     = authManager;
        this.tokenProvider   = tokenProvider;
        this.jwtProperties   = jwtProperties;
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher  = eventPublisher;
    }

    // ─── Registro público ─────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Public registration attempt for: {}", request.email());

        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        // Papel fixo CUSTOMER — nunca pode ser alterado por esta rota
        var user  = User.create(request.name(), email,
                passwordEncoder.encode(request.password()), UserRole.CUSTOMER);
        var saved = userRepository.save(user);

        eventPublisher.publish(UserRegisteredEvent.of(saved.getId(), saved.getEmail(), saved.getRole()));
        log.info("User self-registered id={} email={}", saved.getId(), email);

        // Gera JWT imediatamente — usuário já está "logado" após o cadastro
        var accessToken  = tokenProvider.generateAccessToken(saved.getId(), saved.getEmail(), saved.getRole().name());
        var refreshToken = tokenProvider.generateRefreshToken(saved.getId());

        return AuthResponse.of(accessToken, refreshToken,
                jwtProperties.accessTokenExpirationMs(),
                saved.getId(), saved.getEmail(), saved.getRole());
    }

    // ─── Login ────────────────────────────────────────────────────────────

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.email());

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().toLowerCase().trim(), request.password())
        );

        var user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        if (!user.isActive()) {
            throw new BusinessRuleException("Account is deactivated. Contact support.");
        }

        var accessToken  = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        var refreshToken = tokenProvider.generateRefreshToken(user.getId());

        log.info("Login successful for userId={}", user.getId());
        return AuthResponse.of(accessToken, refreshToken,
                jwtProperties.accessTokenExpirationMs(),
                user.getId(), user.getEmail(), user.getRole());
    }

    // ─── Refresh token ────────────────────────────────────────────────────

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.refreshToken())) {
            throw new AuthenticationException("Invalid or expired refresh token") {};
        }

        var userId = tokenProvider.extractUserId(request.refreshToken());
        var user   = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.isActive()) {
            throw new BusinessRuleException("Account is deactivated.");
        }

        var accessToken  = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        var refreshToken = tokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.of(accessToken, refreshToken,
                jwtProperties.accessTokenExpirationMs(),
                user.getId(), user.getEmail(), user.getRole());
    }

    // ─── Troca de senha ───────────────────────────────────────────────────

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Valida a senha atual antes de permitir a troca
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Current password is incorrect.");
        }

        // Verifica que a nova senha é diferente da atual
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("New password must be different from the current password.");
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed for userId={}", userId);
    }

    // ─── Perfil ───────────────────────────────────────────────────────────

    @Override
    public UserResponse me(UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::fromDomain)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}