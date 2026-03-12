package com.nexora.application.dto.product;

import com.nexora.domain.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta de produto.
 * Inclui factory method fromDomain() para manter o mapeamento explícito e
 * garantir que a entidade de domínio nunca seja exposta na API.
 */
public record ProductResponse(
        UUID id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        String currency,
        int stockQuantity,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProductResponse fromDomain(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getPrice().amount(),
                product.getPrice().currency(),
                product.getStock().value(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}