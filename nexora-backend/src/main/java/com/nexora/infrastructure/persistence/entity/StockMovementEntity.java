package com.nexora.infrastructure.persistence.entity;

import com.nexora.domain.model.StockMovement;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements",
        indexes = {
                @Index(name = "idx_stock_movements_product", columnList = "product_id"),
                @Index(name = "idx_stock_movements_occurred", columnList = "occurred_at")
        })
public class StockMovementEntity {

    @Id                              private UUID              id;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10) private StockMovement.Type type;
    @Column(nullable = false)        private int               quantity;
    @Column(nullable = false)        private int               stockBefore;
    @Column(nullable = false)        private int               stockAfter;
    @Column(length = 100)            private String            reason;
    @Column(name = "reference_id")   private UUID              referenceId;
    @Column(name = "performed_by")   private UUID              performedBy;
    @Column(nullable = false, updatable = false) private Instant occurredAt;

    protected StockMovementEntity() {}

    public static StockMovementEntity fromDomain(StockMovement m) {
        var e = new StockMovementEntity();
        e.id = m.getId(); e.productId = m.getProductId(); e.type = m.getType();
        e.quantity = m.getQuantity(); e.stockBefore = m.getStockBefore();
        e.stockAfter = m.getStockAfter(); e.reason = m.getReason();
        e.referenceId = m.getReferenceId(); e.performedBy = m.getPerformedBy();
        e.occurredAt = m.getOccurredAt();
        return e;
    }

    public StockMovement toDomain() {
        return StockMovement.reconstitute(id, productId, type, quantity, stockBefore,
                stockAfter, reason, referenceId, performedBy, occurredAt);
    }
}