        package com.nexora.application.dto.product;

import jakarta.validation.constraints.*;
        import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @Size(max = 500) String description,
        @NotBlank @Pattern(regexp = "^[A-Z0-9\\-]{3,20}$") String sku,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Min(0) int initialStock,
        UUID categoryId   // opcional
) {}