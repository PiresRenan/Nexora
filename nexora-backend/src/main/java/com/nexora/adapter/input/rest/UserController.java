package com.nexora.adapter.input.rest;

import com.nexora.application.dto.user.CreateUserRequest;
import com.nexora.application.dto.user.UpdateUserRequest;
import com.nexora.application.dto.user.UserResponse;
import com.nexora.application.usecase.UserUseCase;
import com.nexora.domain.model.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Gerenciamento de usuários por staff (MANAGER/ADMIN).
 *
 * Para auto-cadastro público de clientes, use POST /api/v1/auth/register.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Gestão de usuários por MANAGER/ADMIN")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @PostMapping
    @Operation(
            summary = "Criar usuário (MANAGER+)",
            description = "Cria usuário com papel explícito. Para criar clientes sem autenticação, use POST /api/v1/auth/register.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Usuário criado"),
                    @ApiResponse(responseCode = "409", description = "Email já cadastrado")
            }
    )
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        var response = userUseCase.createUser(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar usuários (MANAGER+)", description = "Filtre por papel com `?role=CUSTOMER`")
    public ResponseEntity<List<UserResponse>> findAll(
            @RequestParam(required = false) UserRole role
    ) {
        var users = role != null
                ? userUseCase.findAllByRole(role)
                : userUseCase.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(userUseCase.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar nome e email do usuário")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userUseCase.updateUser(id, request));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Alterar papel do usuário")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable UUID id,
            @RequestParam UserRole role
    ) {
        return ResponseEntity.ok(userUseCase.changeRole(id, role));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Desativar usuário (soft delete)",
            description = "O usuário perde o acesso mas seu histórico de pedidos é preservado."
    )
    public void deleteUser(@PathVariable UUID id) {
        userUseCase.deleteUser(id);
    }

    @PatchMapping("/{id}/activate")
    @Operation(
            summary = "Reativar usuário desativado",
            description = "Restaura o acesso de um usuário previamente desativado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Usuário reativado"),
                    @ApiResponse(responseCode = "422", description = "Usuário já está ativo")
            }
    )
    public ResponseEntity<UserResponse> activateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userUseCase.activateUser(id));
    }
}