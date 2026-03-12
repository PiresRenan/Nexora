package com.nexora.adapter.output.persistence;

import com.nexora.adapter.output.persistence.jpa.StockMovementJpaRepository;
import com.nexora.domain.model.StockMovement;
import com.nexora.domain.repository.StockMovementRepository;
import com.nexora.infrastructure.persistence.entity.StockMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class StockMovementPersistenceAdapter implements StockMovementRepository {

    private final StockMovementJpaRepository jpa;
    public StockMovementPersistenceAdapter(StockMovementJpaRepository jpa) { this.jpa = jpa; }

    @Override public StockMovement save(StockMovement m)                       { return jpa.save(StockMovementEntity.fromDomain(m)).toDomain(); }
    @Override public Page<StockMovement> findByProductId(UUID id, Pageable p)  { return jpa.findByProductId(id, p).map(StockMovementEntity::toDomain); }
    @Override public Page<StockMovement> findAll(Pageable p)                   { return jpa.findAll(p).map(StockMovementEntity::toDomain); }
}