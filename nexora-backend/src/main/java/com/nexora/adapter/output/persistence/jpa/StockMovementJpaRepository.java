package com.nexora.adapter.output.persistence.jpa;

import com.nexora.infrastructure.persistence.entity.StockMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StockMovementJpaRepository extends JpaRepository<StockMovementEntity, UUID> {
    Page<StockMovementEntity> findByProductId(UUID productId, Pageable pageable);
}