package com.nexora.application.dto.product;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO de criação de produto.
 * Melhoria: record do Java 16+ garante imutabilidade e reduz boilerplate.
 * Bean Validation nas anotações do record.
 */
public record CreateProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotBlank(message = "SKU is required")
        @Pattern(regexp = "^[A-Z0-9\\-]{3,20}$", message = "SKU must contain only uppercase letters, digits and hyphens (3-20 chars)")
        String sku,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
        BigDecimal price,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code (e.g. BRL, USD)")
        String currency,

        @Min(value = 0, message = "Initial stock cannot be negative")
        int initialStock
) {}