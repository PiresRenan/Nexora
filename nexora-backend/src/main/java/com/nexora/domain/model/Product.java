package com.nexora.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio Product — Fase 2: suporte a categoria.
 */
public class Product {

    private final UUID id;
    private String name;
    private String description;
    private String sku;
    private Money price;
    private StockQuantity stock;
    private UUID categoryId;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public static Product create(String name, String description, String sku,
                                 Money price, StockQuantity stock) {
        return create(name, description, sku, price, stock, null);
    }

    public static Product create(String name, String description, String sku,
                                 Money price, StockQuantity stock, UUID categoryId) {
        return new Product(UUID.randomUUID(), name, description, sku,
                price, stock, categoryId, true, Instant.now(), Instant.now());
    }

    public static Product reconstitute(
            UUID id, String name, String description, String sku,
            Money price, StockQuantity stock, boolean active,
            Instant createdAt, Instant updatedAt) {
        return new Product(id, name, description, sku, price, stock,
                null, active, createdAt, updatedAt);
    }

    public static Product reconstitute(
            UUID id, String name, String description, String sku,
            Money price, StockQuantity stock, UUID categoryId, boolean active,
            Instant createdAt, Instant updatedAt) {
        return new Product(id, name, description, sku, price, stock,
                categoryId, active, createdAt, updatedAt);
    }

    private Product(UUID id, String name, String description, String sku,
                    Money price, StockQuantity stock, UUID categoryId, boolean active,
                    Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id);
        this.name = requireNonBlank(name, "Product name");
        this.description = description;
        this.sku = requireNonBlank(sku, "Product SKU");
        this.price = Objects.requireNonNull(price);
        this.stock = Objects.requireNonNull(stock);
        this.categoryId = categoryId;
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void updateDetails(String name, String description, Money price) {
        this.name = requireNonBlank(name, "Product name");
        this.description = description;
        this.price = Objects.requireNonNull(price);
        this.updatedAt = Instant.now();
    }

    public void assignCategory(UUID categoryId) { this.categoryId = categoryId; this.updatedAt = Instant.now(); }
    public void removeCategory()                { this.categoryId = null;       this.updatedAt = Instant.now(); }

    public void replenishStock(int quantity) { this.stock = this.stock.add(quantity);      this.updatedAt = Instant.now(); }
    public void withdrawStock(int quantity)  { this.stock = this.stock.subtract(quantity); this.updatedAt = Instant.now(); }

    public void deactivate() { this.active = false; this.updatedAt = Instant.now(); }
    public void activate()   { this.active = true;  this.updatedAt = Instant.now(); }

    public boolean isAvailable(int requestedQuantity) {
        return this.active && this.stock.isAvailableFor(requestedQuantity);
    }

    public UUID          getId()          { return id; }
    public String        getName()        { return name; }
    public String        getDescription() { return description; }
    public String        getSku()         { return sku; }
    public Money         getPrice()       { return price; }
    public StockQuantity getStock()       { return stock; }
    public UUID          getCategoryId()  { return categoryId; }
    public boolean       isActive()       { return active; }
    public Instant       getCreatedAt()   { return createdAt; }
    public Instant       getUpdatedAt()   { return updatedAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product p)) return false;
        return Objects.equals(id, p.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() {
        return "Product{id=%s, sku='%s', active=%s}".formatted(id, sku, active);
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return v;
    }
}