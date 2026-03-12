package com.nexora.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio: Categoria de produto.
 * Permite organizar o catálogo e futuras filtragens/relatórios.
 */
public class Category {

    private final UUID id;
    private String name;
    private String description;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public static Category create(String name, String description) {
        return new Category(
                UUID.randomUUID(), name, description,
                true, Instant.now(), Instant.now()
        );
    }

    public static Category reconstitute(
            UUID id, String name, String description,
            boolean active, Instant createdAt, Instant updatedAt
    ) {
        return new Category(id, name, description, active, createdAt, updatedAt);
    }

    private Category(UUID id, String name, String description,
                     boolean active, Instant createdAt, Instant updatedAt) {
        this.id          = Objects.requireNonNull(id);
        this.name        = requireNonBlank(name, "Category name");
        this.description = description;
        this.active      = active;
        this.createdAt   = Objects.requireNonNull(createdAt);
        this.updatedAt   = Objects.requireNonNull(updatedAt);
    }

    public void update(String name, String description) {
        this.name        = requireNonBlank(name, "Category name");
        this.description = description;
        this.updatedAt   = Instant.now();
    }

    public void deactivate() { this.active = false; this.updatedAt = Instant.now(); }
    public void activate()   { this.active = true;  this.updatedAt = Instant.now(); }

    public UUID    getId()          { return id; }
    public String  getName()        { return name; }
    public String  getDescription() { return description; }
    public boolean isActive()       { return active; }
    public Instant getCreatedAt()   { return createdAt; }
    public Instant getUpdatedAt()   { return updatedAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category c)) return false;
        return Objects.equals(id, c.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.isBlank())
            throw new IllegalArgumentException(field + " cannot be blank");
        return v;
    }
}