package com.nexora.application.usecase;

import com.nexora.application.dto.auth.AuthResponse;
import com.nexora.application.dto.auth.LoginRequest;
import com.nexora.application.dto.auth.RefreshTokenRequest;
import com.nexora.application.dto.user.UserResponse;

public interface AuthUseCase {
    AuthResponse    login(LoginRequest request);
    AuthResponse    refresh(RefreshTokenRequest request);
    UserResponse    me(java.util.UUID userId);
}