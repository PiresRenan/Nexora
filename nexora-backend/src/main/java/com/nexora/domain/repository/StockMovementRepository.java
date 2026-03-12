package com.nexora.domain.repository;

import com.nexora.domain.model.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockMovementRepository {
    StockMovement save(StockMovement movement);
    Page<StockMovement> findByProductId(UUID productId, Pageable pageable);
    Page<StockMovement> findAll(Pageable pageable);
}