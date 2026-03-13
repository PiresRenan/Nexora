package com.nexora.application.usecase;

import com.nexora.application.dto.order.CreateOrderRequest;
import com.nexora.application.dto.order.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderUseCase {
    OrderResponse   createOrder(UUID customerId, CreateOrderRequest request);
    OrderResponse   confirmOrder(UUID orderId, UUID performedBy);
    OrderResponse   shipOrder(UUID orderId, UUID performedBy);
    OrderResponse   deliverOrder(UUID orderId, UUID performedBy);
    OrderResponse   cancelOrder(UUID orderId, UUID requestedBy, String reason);
    OrderResponse   findById(UUID orderId, UUID requestedBy);
    Page<OrderResponse> findMyOrders(UUID customerId, Pageable pageable);
    Page<OrderResponse> findAll(Pageable pageable);
}
