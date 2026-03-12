package com.nexora.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de auditoria: movimento de estoque.
 * Imutável — representa um fato histórico que nunca muda.
 * Registra toda entrada e saída de estoque com motivo e referência.
 */
public class StockMovement {

    public enum Type { ENTRY, EXIT }

    private final UUID id;
    private final UUID productId;
    private final Type type;
    private final int quantity;
    private final int stockBefore;
    private final int stockAfter;
    private final String reason;        // ex: "ORDER_CONFIRMED", "MANUAL_REPLENISHMENT"
    private final UUID referenceId;     // ex: orderId (nullable)
    private final UUID performedBy;     // userId que executou
    private final Instant occurredAt;

    public static StockMovement entry(
            UUID productId, int qty, int before, int after,
            String reason, UUID referenceId, UUID performedBy
    ) {
        return new StockMovement(UUID.randomUUID(), productId, Type.ENTRY,
                qty, before, after, reason, referenceId, performedBy, Instant.now());
    }

    public static StockMovement exit(
            UUID productId, int qty, int before, int after,
            String reason, UUID referenceId, UUID performedBy
    ) {
        return new StockMovement(UUID.randomUUID(), productId, Type.EXIT,
                qty, before, after, reason, referenceId, performedBy, Instant.now());
    }

    public static StockMovement reconstitute(
            UUID id, UUID productId, Type type, int quantity,
            int stockBefore, int stockAfter, String reason,
            UUID referenceId, UUID performedBy, Instant occurredAt
    ) {
        return new StockMovement(id, productId, type, quantity,
                stockBefore, stockAfter, reason, referenceId, performedBy, occurredAt);
    }

    private StockMovement(UUID id, UUID productId, Type type, int quantity,
                          int stockBefore, int stockAfter, String reason,
                          UUID referenceId, UUID performedBy, Instant occurredAt) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.id          = Objects.requireNonNull(id);
        this.productId   = Objects.requireNonNull(productId);
        this.type        = Objects.requireNonNull(type);
        this.quantity    = quantity;
        this.stockBefore = stockBefore;
        this.stockAfter  = stockAfter;
        this.reason      = reason;
        this.referenceId = referenceId;
        this.performedBy = performedBy;
        this.occurredAt  = Objects.requireNonNull(occurredAt);
    }

    public UUID    getId()          { return id; }
    public UUID    getProductId()   { return productId; }
    public Type    getType()        { return type; }
    public int     getQuantity()    { return quantity; }
    public int     getStockBefore() { return stockBefore; }
    public int     getStockAfter()  { return stockAfter; }
    public String  getReason()      { return reason; }
    public UUID    getReferenceId() { return referenceId; }
    public UUID    getPerformedBy() { return performedBy; }
    public Instant getOccurredAt()  { return occurredAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockMovement m)) return false;
        return Objects.equals(id, m.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}