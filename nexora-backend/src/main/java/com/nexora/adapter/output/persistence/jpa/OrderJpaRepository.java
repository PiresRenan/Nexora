package com.nexora.adapter.output.persistence.jpa;

import com.nexora.domain.model.OrderStatus;
import com.nexora.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    Page<OrderEntity> findByCustomerId(UUID customerId, Pageable pageable);
    Page<OrderEntity> findByStatus(OrderStatus status, Pageable pageable);
}