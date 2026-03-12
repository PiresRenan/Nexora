package com.nexora.adapter.input.rest;

import com.nexora.application.dto.auth.AuthResponse;
import com.nexora.application.dto.auth.LoginRequest;
import com.nexora.application.dto.auth.RefreshTokenRequest;
import com.nexora.application.dto.user.UserResponse;
import com.nexora.application.usecase.AuthUseCase;
import com.nexora.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login, token refresh and user profile")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authUseCase.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authUseCase.refresh(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get authenticated user profile",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> me(@CurrentUser UUID userId) {
        return ResponseEntity.ok(authUseCase.me(userId));
    }
}