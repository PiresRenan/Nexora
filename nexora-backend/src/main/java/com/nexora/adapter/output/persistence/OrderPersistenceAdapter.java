package com.nexora.adapter.output.persistence;

import com.nexora.adapter.output.persistence.jpa.OrderJpaRepository;
import com.nexora.domain.model.Order;
import com.nexora.domain.model.OrderStatus;
import com.nexora.domain.repository.OrderRepository;
import com.nexora.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderPersistenceAdapter implements OrderRepository {

    private final OrderJpaRepository jpa;
    public OrderPersistenceAdapter(OrderJpaRepository jpa) { this.jpa = jpa; }

    @Override public Order save(Order o)                                       { return jpa.save(OrderEntity.fromDomain(o)).toDomain(); }
    @Override public Optional<Order> findById(UUID id)                         { return jpa.findById(id).map(OrderEntity::toDomain); }
    @Override public Page<Order> findByCustomerId(UUID cId, Pageable p)        { return jpa.findByCustomerId(cId, p).map(OrderEntity::toDomain); }
    @Override public Page<Order> findByStatus(OrderStatus s, Pageable p)       { return jpa.findByStatus(s, p).map(OrderEntity::toDomain); }
    @Override public Page<Order> findAll(Pageable p)                           { return jpa.findAll(p).map(OrderEntity::toDomain); }
}