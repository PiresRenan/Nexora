package com.nexora.application.dto.category;

import com.nexora.domain.model.Category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(UUID id, String name, String description, boolean active,
                               Instant createdAt, Instant updatedAt) {
    public static CategoryResponse fromDomain(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(),
                c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
    }
}