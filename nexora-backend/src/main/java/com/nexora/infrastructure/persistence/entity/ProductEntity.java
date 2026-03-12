package com.nexora.infrastructure.persistence.entity;

import com.nexora.domain.model.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_sku",      columnList = "sku",         unique = true),
        @Index(name = "idx_products_active",   columnList = "active"),
        @Index(name = "idx_products_category", columnList = "category_id")
})
public class ProductEntity {

    @Id @Column(nullable = false, updatable = false) private UUID       id;
    @Column(nullable = false, length = 100)          private String     name;
    @Column(length = 500)                            private String     description;
    @Column(nullable = false, length = 20, unique = true) private String sku;
    @Column(nullable = false, precision = 12, scale = 2)  private BigDecimal price;
    @Column(nullable = false, length = 3)            private String     currency;
    @Column(nullable = false)                        private int        stockQuantity;
    @Column(name = "category_id")                    private UUID       categoryId;
    @Column(nullable = false)                        private boolean    active;
    @Column(nullable = false, updatable = false)     private Instant    createdAt;
    @Column(nullable = false)                        private Instant    updatedAt;

    protected ProductEntity() {}

    public static ProductEntity fromDomain(Product p) {
        var e = new ProductEntity();
        e.id = p.getId(); e.name = p.getName(); e.description = p.getDescription();
        e.sku = p.getSku(); e.price = p.getPrice().amount(); e.currency = p.getPrice().currency();
        e.stockQuantity = p.getStock().value(); e.categoryId = p.getCategoryId();
        e.active = p.isActive(); e.createdAt = p.getCreatedAt(); e.updatedAt = p.getUpdatedAt();
        return e;
    }

    public Product toDomain() {
        return Product.reconstitute(id, name, description, sku,
                Money.of(price, currency), StockQuantity.of(stockQuantity),
                categoryId, active, createdAt, updatedAt);
    }

    public UUID    getId()     { return id; }
    public String  getSku()    { return sku; }
    public boolean isActive()  { return active; }
    public UUID    getCategoryId() { return categoryId; }
}