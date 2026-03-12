package com.nexora.application.dto.category;

import com.nexora.domain.model.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank @Size(min = 2, max = 80) String name,
        @Size(max = 300) String description
) {}