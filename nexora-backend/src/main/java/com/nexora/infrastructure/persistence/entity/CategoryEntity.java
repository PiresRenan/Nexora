package com.nexora.infrastructure.persistence.entity;

import com.nexora.domain.model.Category;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories",
        indexes = @Index(name = "idx_categories_name", columnList = "name", unique = true))
public class CategoryEntity {

    @Id                              private UUID    id;
    @Column(nullable = false, length = 80) private String  name;
    @Column(length = 300)            private String  description;
    @Column(nullable = false)        private boolean active;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false)        private Instant updatedAt;

    protected CategoryEntity() {}

    public static CategoryEntity fromDomain(Category c) {
        var e = new CategoryEntity();
        e.id = c.getId(); e.name = c.getName(); e.description = c.getDescription();
        e.active = c.isActive(); e.createdAt = c.getCreatedAt(); e.updatedAt = c.getUpdatedAt();
        return e;
    }

    public Category toDomain() {
        return Category.reconstitute(id, name, description, active, createdAt, updatedAt);
    }

    public UUID getId() { return id; }
}