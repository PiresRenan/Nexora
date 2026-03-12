package com.nexora.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de domínio Product — sem dependências de framework.
 * Toda lógica de negócio referente ao produto vive aqui.
 * <p>
 * Melhoria: dois construtores explícitos:
 * - Criação nova (gera UUID + timestamps)
 * - Reconstituição da persistência (recebe todos os campos)
 * Isso torna as intenções claras e evita construtores ambíguos.
 */
public class Product {

    private final UUID id;
    private String name;
    private String description;
    private String sku;
    private Money price;
    private StockQuantity stock;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    // ─── Criação de novo produto ───────────────────────────────────────────

    public static Product create(String name, String description, String sku, Money price, StockQuantity stock) {
        return new Product(
                UUID.randomUUID(),
                name, description, sku,
                price, stock,
                true,
                Instant.now(), Instant.now()
        );
    }

    // ─── Reconstituição da persistência ───────────────────────────────────

    public static Product reconstitute(
            UUID id, String name, String description, String sku,
            Money price, StockQuantity stock, boolean active,
            Instant createdAt, Instant updatedAt
    ) {
        return new Product(id, name, description, sku, price, stock, active, createdAt, updatedAt);
    }

    // ─── Construtor privado ────────────────────────────────────────────────

    private Product(
            UUID id, String name, String description, String sku,
            Money price, StockQuantity stock, boolean active,
            Instant createdAt, Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Product id cannot be null");
        this.name = requireNonBlank(name, "Product name");
        this.description = description;
        this.sku = requireNonBlank(sku, "Product SKU");
        this.price = Objects.requireNonNull(price, "Product price cannot be null");
        this.stock = Objects.requireNonNull(stock, "Product stock cannot be null");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    // ─── Comportamentos de domínio ─────────────────────────────────────────

    public void updateDetails(String name, String description, Money price) {
        this.name = requireNonBlank(name, "Product name");
        this.description = description;
        this.price = Objects.requireNonNull(price, "Product price cannot be null");
        this.updatedAt = Instant.now();
    }

    public void replenishStock(int quantity) {
        this.stock = this.stock.add(quantity);
        this.updatedAt = Instant.now();
    }

    public void withdrawStock(int quantity) {
        this.stock = this.stock.subtract(quantity);
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public boolean isAvailable(int requestedQuantity) {
        return this.active && this.stock.isAvailableFor(requestedQuantity);
    }

    // ─── Getters ───────────────────────────────────────────────────────────

    public UUID getId()             { return id; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public String getSku()          { return sku; }
    public Money getPrice()         { return price; }
    public StockQuantity getStock() { return stock; }
    public boolean isActive()       { return active; }
    public Instant getCreatedAt()   { return createdAt; }
    public Instant getUpdatedAt()   { return updatedAt; }

    // ─── Igualdade por identidade ──────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Product{id=%s, name='%s', sku='%s', active=%s}".formatted(id, name, sku, active);
    }

    // ─── Helper de validação ───────────────────────────────────────────────

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }
}