package com.nexora.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Entidade de domínio User — sem dependências de framework.
 * A senha já chega como hash — nunca armazenamos senha em texto plano.
 */
public class User {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private final UUID id;
    private String name;
    private String email;
    private String passwordHash;
    private UserRole role;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    // ─── Factory Methods ───────────────────────────────────────────────────

    public static User create(String name, String email, String passwordHash, UserRole role) {
        return new User(
                UUID.randomUUID(),
                name, email, passwordHash, role,
                true,
                Instant.now(), Instant.now()
        );
    }

    public static User reconstitute(
            UUID id, String name, String email, String passwordHash,
            UserRole role, boolean active, Instant createdAt, Instant updatedAt
    ) {
        return new User(id, name, email, passwordHash, role, active, createdAt, updatedAt);
    }

    // ─── Construtor privado ────────────────────────────────────────────────

    private User(
            UUID id, String name, String email, String passwordHash,
            UserRole role, boolean active, Instant createdAt, Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "User id cannot be null");
        this.name = requireNonBlank(name, "User name");
        this.email = validateEmail(email);
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash cannot be null");
        this.role = Objects.requireNonNull(role, "User role cannot be null");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    // ─── Comportamentos de domínio ─────────────────────────────────────────

    public void updateProfile(String name, String email) {
        this.name = requireNonBlank(name, "User name");
        this.email = validateEmail(email);
        this.updatedAt = Instant.now();
    }

    public void changeRole(UserRole newRole) {
        this.role = Objects.requireNonNull(newRole, "Role cannot be null");
        this.updatedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "Password hash cannot be null");
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public boolean canManage(UserRole targetRole) {
        return this.role.hasPermissionOf(targetRole);
    }

    // ─── Getters ───────────────────────────────────────────────────────────

    public UUID getId()            { return id; }
    public String getName()        { return name; }
    public String getEmail()       { return email; }
    public String getPasswordHash(){ return passwordHash; }
    public UserRole getRole()      { return role; }
    public boolean isActive()      { return active; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }

    // ─── Igualdade por identidade ──────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{id=%s, email='%s', role=%s, active=%s}".formatted(id, email, role, active);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }

    private static String validateEmail(String email) {
        requireNonBlank(email, "Email");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        return email.toLowerCase().trim();
    }
}