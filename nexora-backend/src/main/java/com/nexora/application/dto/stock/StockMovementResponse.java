package com.nexora.application.dto.stock;

import com.nexora.domain.model.StockMovement;

import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
    UUID               id,
    UUID               productId,
    StockMovement.Type type,
    int                quantity,
    int                stockBefore,
    int                stockAfter,
    String             reason,
    UUID               referenceId,
    UUID               performedBy,
    Instant            occurredAt
) {
    public static StockMovementResponse fromDomain(StockMovement m) {
        return new StockMovementResponse(
            m.getId(), m.getProductId(), m.getType(),
            m.getQuantity(), m.getStockBefore(), m.getStockAfter(),
            m.getReason(), m.getReferenceId(), m.getPerformedBy(), m.getOccurredAt()
        );
    }
}