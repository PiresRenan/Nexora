package com.nexora.application.service;

import com.nexora.application.dto.order.CreateOrderRequest;
import com.nexora.application.dto.order.OrderResponse;
import com.nexora.application.usecase.OrderUseCase;
import com.nexora.domain.event.OrderCancelledEvent;
import com.nexora.domain.event.OrderConfirmedEvent;
import com.nexora.domain.exception.BusinessRuleException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.*;
import com.nexora.domain.port.EventPublisher;
import com.nexora.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orquestra o ciclo de vida completo do pedido.
 *
 * Responsabilidades:
 *  1. Criar pedido validando disponibilidade de estoque no domínio
 *  2. Confirmação: decrementar estoque + registrar StockMovement + publicar evento
 *  3. Cancelamento (se CONFIRMED+): devolver estoque + StockMovement + publicar evento
 *  4. Controle de acesso: cliente só visualiza/cancela os próprios pedidos
 *
 * BUG CORRIGIDO: A verificação anterior usava .ordinal() para comparar status,
 * o que dependia da ordem de declaração do enum. Substituído por verificação
 * explícita dos estados onde o estoque já foi comprometido (CONFIRMED, SHIPPED).
 */
@Service
@Transactional
public class OrderApplicationService implements OrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

    private final OrderRepository         orderRepository;
    private final ProductRepository       productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository          userRepository;
    private final EventPublisher          eventPublisher;

    public OrderApplicationService(
            OrderRepository         orderRepository,
            ProductRepository       productRepository,
            StockMovementRepository stockMovementRepository,
            UserRepository          userRepository,
            EventPublisher          eventPublisher
    ) {
        this.orderRepository         = orderRepository;
        this.productRepository       = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository          = userRepository;
        this.eventPublisher          = eventPublisher;
    }

    // ─── Criação ───────────────────────────────────────────────────────────

    @Override
    public OrderResponse createOrder(UUID customerId, CreateOrderRequest request) {
        log.info("Creating order for customer={}", customerId);

        userRepository.findById(customerId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User", customerId));

        var order = Order.create(customerId, request.notes());

        for (var itemReq : request.items()) {
            var product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.productId()));
            order.addItem(product, itemReq.quantity());
        }

        var saved = orderRepository.save(order);
        log.info("Order created id={} items={}", saved.getId(), saved.getItems().size());
        return OrderResponse.fromDomain(saved);
    }

    // ─── Confirmação ───────────────────────────────────────────────────────

    @Override
    public OrderResponse confirmOrder(UUID orderId, UUID performedBy) {
        log.info("Confirming order={} by={}", orderId, performedBy);

        var order = findOrThrow(orderId);
        order.validateForConfirmation();

        for (var item : order.getItems()) {
            var product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.productId()));

            if (!product.isAvailable(item.quantity())) {
                throw new BusinessRuleException(
                        "Insufficient stock for '%s': available=%d, required=%d"
                                .formatted(product.getName(), product.getStock().value(), item.quantity())
                );
            }

            int before = product.getStock().value();
            product.withdrawStock(item.quantity());
            productRepository.save(product);

            stockMovementRepository.save(StockMovement.exit(
                    product.getId(), item.quantity(), before, product.getStock().value(),
                    "ORDER_CONFIRMED", order.getId(), performedBy
            ));
        }

        order.confirm();
        var saved = orderRepository.save(order);

        // Publica evento com snapshot completo do pedido
        var items = saved.getItems().stream()
                .map(i -> new OrderConfirmedEvent.ItemSnapshot(
                        i.productId(), i.productSku(), i.quantity(), i.unitPrice().amount()))
                .toList();
        eventPublisher.publish(OrderConfirmedEvent.of(
                saved.getId(), saved.getCustomerId(),
                saved.calculateTotal().amount(), saved.calculateTotal().currency(), items
        ));

        return OrderResponse.fromDomain(saved);
    }

    // ─── Transições de status ──────────────────────────────────────────────

    @Override
    public OrderResponse shipOrder(UUID orderId, UUID performedBy) {
        var order = findOrThrow(orderId);
        order.ship();
        return OrderResponse.fromDomain(orderRepository.save(order));
    }

    @Override
    public OrderResponse deliverOrder(UUID orderId, UUID performedBy) {
        var order = findOrThrow(orderId);
        order.deliver();
        return OrderResponse.fromDomain(orderRepository.save(order));
    }

    // ─── Cancelamento ──────────────────────────────────────────────────────

    @Override
    public OrderResponse cancelOrder(UUID orderId, UUID requestedBy, String reason) {
        log.info("Cancelling order={} by={}", orderId, requestedBy);

        var order = findOrThrow(orderId);

        // Estoque só deve ser devolvido se já foi comprometido na confirmação.
        // BUG FIX: verificação explícita dos estados onde estoque está comprometido,
        // não comparação por ordinal() que dependia da ordem de declaração do enum.
        boolean stockWasReserved = order.getStatus() == OrderStatus.CONFIRMED
                || order.getStatus() == OrderStatus.SHIPPED;

        if (stockWasReserved) {
            for (var item : order.getItems()) {
                var product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product", item.productId()));

                int before = product.getStock().value();
                product.replenishStock(item.quantity());
                productRepository.save(product);

                stockMovementRepository.save(StockMovement.entry(
                        product.getId(), item.quantity(), before, product.getStock().value(),
                        "ORDER_CANCELLED", order.getId(), requestedBy
                ));
            }
        }

        order.cancel(reason);
        var saved = orderRepository.save(order);

        eventPublisher.publish(OrderCancelledEvent.of(
                saved.getId(), saved.getCustomerId(), requestedBy, reason));

        return OrderResponse.fromDomain(saved);
    }

    // ─── Consultas ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderResponse findById(UUID orderId, UUID requestedBy) {
        var order = findOrThrow(orderId);

        // Cliente só vê seus próprios pedidos; funcionários veem todos
        if (!order.getCustomerId().equals(requestedBy)) {
            userRepository.findById(requestedBy)
                    .filter(u -> u.getRole().isEmployee())
                    .orElseThrow(() -> new AccessDeniedException("You can only view your own orders"));
        }

        return OrderResponse.fromDomain(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> findMyOrders(UUID customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(OrderResponse::fromDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::fromDomain);
    }

    private Order findOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
}