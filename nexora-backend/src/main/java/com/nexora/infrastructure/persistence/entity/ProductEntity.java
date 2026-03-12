package com.nexora.infrastructure.persistence.entity;

import com.nexora.domain.model.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA de produto — isolada na infraestrutura.
 * Nunca sai desta camada para a API ou domínio.
 * <p>
 * Melhoria: conversores explícitos para Value Objects do domínio
 * nos métodos toDomain() e fromDomain(), em vez de usar @Embedded.
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_sku", columnList = "sku", unique = true),
                @Index(name = "idx_products_active", columnList = "active")
        }
)
public class ProductEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 20, unique = true)
    private String sku;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ProductEntity() {}

    // ─── Conversão Domain ↔ Entity ─────────────────────────────────────────

    public static ProductEntity fromDomain(Product product) {
        var entity = new ProductEntity();
        entity.id = product.getId();
        entity.name = product.getName();
        entity.description = product.getDescription();
        entity.sku = product.getSku();
        entity.price = product.getPrice().amount();
        entity.currency = product.getPrice().currency();
        entity.stockQuantity = product.getStock().value();
        entity.active = product.isActive();
        entity.createdAt = product.getCreatedAt();
        entity.updatedAt = product.getUpdatedAt();
        return entity;
    }

    public Product toDomain() {
        return Product.reconstitute(
                id, name, description, sku,
                Money.of(price, currency),
                StockQuantity.of(stockQuantity),
                active, createdAt, updatedAt
        );
    }

    // ─── Getters ───────────────────────────────────────────────────────────

    public UUID getId()          { return id; }
    public String getName()      { return name; }
    public String getSku()       { return sku; }
    public boolean isActive()    { return active; }
}