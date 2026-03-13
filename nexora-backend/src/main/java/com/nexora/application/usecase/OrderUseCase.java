package com.nexora.application.usecase;

import com.nexora.application.dto.order.CreateOrderRequest;
import com.nexora.application.dto.order.OrderResponse;
import com.nexora.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderUseCase {

    OrderResponse createOrder(UUID customerId, CreateOrderRequest request);

    OrderResponse findById(UUID orderId, UUID requestedBy);

    Page<OrderResponse> findMyOrders(UUID customerId, Pageable pageable);

    /** Lista todos os pedidos. Filtro opcional por status. */
    Page<OrderResponse> findAll(Pageable pageable);

    /** Lista pedidos filtrados por status — MANAGER/ADMIN. */
    Page<OrderResponse> findByStatus(OrderStatus status, Pageable pageable);

    OrderResponse confirmOrder(UUID orderId, UUID performedBy);

    OrderResponse shipOrder(UUID orderId, UUID performedBy);

    OrderResponse deliverOrder(UUID orderId, UUID performedBy);

    OrderResponse cancelOrder(UUID orderId, UUID requestedBy, String reason);
}