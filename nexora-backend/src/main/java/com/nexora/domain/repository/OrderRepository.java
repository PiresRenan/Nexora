package com.nexora.domain.repository;

import com.nexora.domain.model.Order;
import com.nexora.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Output Port para pedidos.
 * Usa Spring Pageable diretamente — pragmático para este contexto.
 * Se a necessidade de isolar da camada de framework surgir, um
 * PageRequest próprio pode ser criado no domínio.
 */
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findAll(Pageable pageable);
}