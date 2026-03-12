package com.nexora.infrastructure.persistence.entity;

import com.nexora.domain.model.User;
import com.nexora.domain.model.UserRole;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA de usuário — isolada na infraestrutura.
 */
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true),
                @Index(name = "idx_users_role", columnList = "role"),
                @Index(name = "idx_users_active", columnList = "active")
        }
)
public class UserEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserEntity() {}

    // ─── Conversão Domain ↔ Entity ─────────────────────────────────────────

    public static UserEntity fromDomain(User user) {
        var entity = new UserEntity();
        entity.id = user.getId();
        entity.name = user.getName();
        entity.email = user.getEmail();
        entity.passwordHash = user.getPasswordHash();
        entity.role = user.getRole();
        entity.active = user.isActive();
        entity.createdAt = user.getCreatedAt();
        entity.updatedAt = user.getUpdatedAt();
        return entity;
    }

    public User toDomain() {
        return User.reconstitute(
                id, name, email, passwordHash,
                role, active, createdAt, updatedAt
        );
    }

    // ─── Getters ───────────────────────────────────────────────────────────

    public UUID getId()       { return id; }
    public String getEmail()  { return email; }
    public UserRole getRole() { return role; }
    public boolean isActive() { return active; }
}