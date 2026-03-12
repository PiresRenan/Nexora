package com.nexora.infrastructure.persistence.entity;

import com.nexora.domain.model.Order;
import com.nexora.domain.model.OrderStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders",
        indexes = {
                @Index(name = "idx_orders_customer", columnList = "customer_id"),
                @Index(name = "idx_orders_status",   columnList = "status")
        })
public class OrderEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items",
            joinColumns = @JoinColumn(name = "order_id"),
            indexes = @Index(name = "idx_order_items_order", columnList = "order_id"))
    private List<OrderItemEmbeddable> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected OrderEntity() {}

    public static OrderEntity fromDomain(Order order) {
        var e = new OrderEntity();
        e.id         = order.getId();
        e.customerId = order.getCustomerId();
        e.items      = order.getItems().stream().map(OrderItemEmbeddable::fromDomain).toList();
        e.status     = order.getStatus();
        e.notes      = order.getNotes();
        e.createdAt  = order.getCreatedAt();
        e.updatedAt  = order.getUpdatedAt();
        return e;
    }

    public Order toDomain() {
        return Order.reconstitute(
                id, customerId,
                items.stream().map(OrderItemEmbeddable::toDomain).toList(),
                status, notes, createdAt, updatedAt
        );
    }

    public UUID getId() { return id; }
}