package com.nexora.adapter.input.rest;

import com.nexora.application.dto.auth.*;
import com.nexora.application.dto.user.UserResponse;
import com.nexora.application.usecase.AuthUseCase;
import com.nexora.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registro público, login, tokens e perfil do usuário")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    /**
     * Auto-cadastro público — não exige autenticação prévia.
     *
     * Cria um usuário com papel CUSTOMER e retorna um JWT imediato,
     * permitindo que o cliente navegue/compre sem etapa extra de login.
     *
     * Para criar funcionários (SELLER, MANAGER, ADMIN), use POST /api/v1/users
     * com um token de MANAGER ou ADMIN.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cadastro público de novo cliente",
            description = """
            Cria uma conta de cliente (papel CUSTOMER) sem necessidade de autenticação.
            Retorna tokens JWT imediatamente — o usuário já está logado após o cadastro.
            
            **Regra de papel:** apenas CUSTOMER pode ser criado por esta rota.
            Para papéis elevados (SELLER, MANAGER, ADMIN), use POST /api/v1/users
            com credenciais de um administrador.
            """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Cadastro realizado com sucesso. Tokens JWT no corpo."),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos (email malformado, senha curta, etc.)"),
                    @ApiResponse(responseCode = "409", description = "Email já cadastrado no sistema")
            }
    )
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        var response = authUseCase.register(request);
        return ResponseEntity
                .created(URI.create("/api/v1/auth/me"))
                .body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login — obtém tokens JWT",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login bem-sucedido"),
                    @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
                    @ApiResponse(responseCode = "422", description = "Conta desativada")
            }
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authUseCase.login(request));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renova access token usando refresh token",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tokens renovados"),
                    @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
            }
    )
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authUseCase.refresh(request));
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Troca de senha pelo próprio usuário",
            description = "Exige a senha atual para confirmar a identidade antes de aceitar a nova.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Senha alterada com sucesso"),
                    @ApiResponse(responseCode = "422", description = "Senha atual incorreta ou nova senha igual à atual")
            }
    )
    public void changePassword(
            @CurrentUser UUID userId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authUseCase.changePassword(userId, request);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Dados do usuário autenticado",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> me(@CurrentUser UUID userId) {
        return ResponseEntity.ok(authUseCase.me(userId));
    }
}