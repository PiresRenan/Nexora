package com.nexora.domain.model;

import com.nexora.domain.exception.BusinessRuleException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root: Pedido.
 *
 * Regras de negócio encapsuladas aqui:
 * - Pedido deve ter ao menos 1 item
 * - Transições de status são controladas pela máquina de estados de OrderStatus
 * - Total é calculado automaticamente a partir dos itens (sem aceitar valor externo)
 * - Só pode adicionar itens quando PENDING
 * - Estoque é controlado externamente (pelo use case), pois Order não conhece ProductRepository
 */
public class Order {

    private final UUID id;
    private final UUID customerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;

    // ─── Factory Methods ───────────────────────────────────────────────────

    public static Order create(UUID customerId, String notes) {
        return new Order(
                UUID.randomUUID(), customerId,
                new ArrayList<>(), OrderStatus.PENDING,
                notes, Instant.now(), Instant.now()
        );
    }

    public static Order reconstitute(
            UUID id, UUID customerId, List<OrderItem> items,
            OrderStatus status, String notes,
            Instant createdAt, Instant updatedAt
    ) {
        return new Order(id, customerId, new ArrayList<>(items), status, notes, createdAt, updatedAt);
    }

    private Order(UUID id, UUID customerId, List<OrderItem> items,
                  OrderStatus status, String notes,
                  Instant createdAt, Instant updatedAt) {
        this.id         = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.items      = items;
        this.status     = Objects.requireNonNull(status);
        this.notes      = notes;
        this.createdAt  = Objects.requireNonNull(createdAt);
        this.updatedAt  = Objects.requireNonNull(updatedAt);
    }

    // ─── Comportamentos de domínio ─────────────────────────────────────────

    public void addItem(Product product, int quantity) {
        if (status != OrderStatus.PENDING) {
            throw new BusinessRuleException(
                    "Cannot add items to order in status: " + status
            );
        }
        if (!product.isAvailable(quantity)) {
            throw new BusinessRuleException(
                    "Product '%s' is not available in the requested quantity (%d)"
                            .formatted(product.getName(), quantity)
            );
        }
        // Consolida item se produto já existe no pedido
        items.stream()
                .filter(i -> i.productId().equals(product.getId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> {
                            items.remove(existing);
                            items.add(new OrderItem(
                                    existing.productId(), existing.productName(), existing.productSku(),
                                    existing.unitPrice(), existing.quantity() + quantity
                            ));
                        },
                        () -> items.add(OrderItem.of(product, quantity))
                );
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        transitionTo(OrderStatus.CONFIRMED);
    }

    public void ship() {
        transitionTo(OrderStatus.SHIPPED);
    }

    public void deliver() {
        transitionTo(OrderStatus.DELIVERED);
    }

    public void cancel(String reason) {
        transitionTo(OrderStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes != null ? this.notes + " | " : "") + "CANCELLED: " + reason;
        }
    }

    public Money calculateTotal() {
        if (items.isEmpty()) {
            throw new BusinessRuleException("Order has no items to calculate total");
        }
        String currency = items.getFirst().unitPrice().currency();
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.zero(currency), Money::add);
    }

    public void validateForConfirmation() {
        if (items.isEmpty()) {
            throw new BusinessRuleException("Cannot confirm order with no items");
        }
    }

    // ─── Getters ───────────────────────────────────────────────────────────

    public UUID             getId()         { return id; }
    public UUID             getCustomerId() { return customerId; }
    public List<OrderItem>  getItems()      { return Collections.unmodifiableList(items); }
    public OrderStatus      getStatus()     { return status; }
    public String           getNotes()      { return notes; }
    public Instant          getCreatedAt()  { return createdAt; }
    public Instant          getUpdatedAt()  { return updatedAt; }

    public boolean isPending()   { return status == OrderStatus.PENDING; }
    public boolean isCancelled() { return status == OrderStatus.CANCELLED; }

    // ─── Private helpers ───────────────────────────────────────────────────

    private void transitionTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessRuleException(
                    "Cannot transition order from %s to %s".formatted(status, next)
            );
        }
        this.status    = next;
        this.updatedAt = Instant.now();
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() {
        return "Order{id=%s, customer=%s, status=%s, items=%d}"
                .formatted(id, customerId, status, items.size());
    }
}