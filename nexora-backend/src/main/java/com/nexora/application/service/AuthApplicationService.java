package com.nexora.application.service;

import com.nexora.application.dto.auth.AuthResponse;
import com.nexora.application.dto.auth.LoginRequest;
import com.nexora.application.dto.auth.RefreshTokenRequest;
import com.nexora.application.dto.user.UserResponse;
import com.nexora.application.usecase.AuthUseCase;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.repository.UserRepository;
import com.nexora.infrastructure.security.JwtProperties;
import com.nexora.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso de autenticação.
 * Delega a verificação de credenciais ao AuthenticationManager do Spring Security
 * e gera os tokens JWT.
 */
@Service
@Transactional(readOnly = true)
public class AuthApplicationService implements AuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthApplicationService.class);

    private final AuthenticationManager authManager;
    private final JwtTokenProvider      tokenProvider;
    private final JwtProperties         jwtProperties;
    private final UserRepository        userRepository;

    public AuthApplicationService(AuthenticationManager authManager,
                                  JwtTokenProvider tokenProvider,
                                  JwtProperties jwtProperties,
                                  UserRepository userRepository) {
        this.authManager    = authManager;
        this.tokenProvider  = tokenProvider;
        this.jwtProperties  = jwtProperties;
        this.userRepository = userRepository;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.email());

        // Lança AuthenticationException se credenciais inválidas
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        var accessToken  = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        var refreshToken = tokenProvider.generateRefreshToken(user.getId());

        log.info("Login successful for userId: {}", user.getId());

        return AuthResponse.of(accessToken, refreshToken,
                jwtProperties.accessTokenExpirationMs(),
                user.getId(), user.getEmail(), user.getRole());
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!tokenProvider.validateToken(request.refreshToken())) {
            throw new AuthenticationException("Invalid or expired refresh token") {};
        }

        var userId = tokenProvider.extractUserId(request.refreshToken());
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        var accessToken  = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        var refreshToken = tokenProvider.generateRefreshToken(user.getId());

        return AuthResponse.of(accessToken, refreshToken,
                jwtProperties.accessTokenExpirationMs(),
                user.getId(), user.getEmail(), user.getRole());
    }

    @Override
    public UserResponse me(UUID userId) {
        return userRepository.findById(userId)
                .map(UserResponse::fromDomain)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}