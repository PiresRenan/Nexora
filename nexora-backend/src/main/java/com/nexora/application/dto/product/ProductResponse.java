        package com.nexora.application.dto.product;

import com.nexora.domain.model.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id, String name, String description, String sku,
        BigDecimal price, String currency,
        int stockQuantity, UUID categoryId,
        boolean active, Instant createdAt, Instant updatedAt
) {
    public static ProductResponse fromDomain(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getSku(),
                p.getPrice().amount(), p.getPrice().currency(),
                p.getStock().value(), p.getCategoryId(),
                p.isActive(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}