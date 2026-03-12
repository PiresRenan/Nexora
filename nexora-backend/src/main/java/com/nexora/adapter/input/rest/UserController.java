package com.nexora.adapter.input.rest;

import com.nexora.application.dto.user.CreateUserRequest;
import com.nexora.application.dto.user.UpdateUserRequest;
import com.nexora.application.dto.user.UserResponse;
import com.nexora.application.usecase.UserUseCase;
import com.nexora.domain.model.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a new user", responses = {
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        var response = userUseCase.createUser(request);
        var location = URI.create("/api/v1/users/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> findAll(
            @RequestParam(required = false) UserRole role
    ) {
        var users = role != null
                ? userUseCase.findAllByRole(role)
                : userUseCase.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find user by ID")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(userUseCase.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userUseCase.updateUser(id, request));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change user role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable UUID id,
            @RequestParam UserRole role
    ) {
        return ResponseEntity.ok(userUseCase.changeRole(id, role));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate user (soft delete)")
    public void deleteUser(@PathVariable UUID id) {
        userUseCase.deleteUser(id);
    }
}