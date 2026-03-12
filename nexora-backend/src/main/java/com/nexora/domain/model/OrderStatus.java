package com.nexora.domain.model;

import java.util.Set;

/**
 * Status de pedido com máquina de estados embutida no domínio.
 * canTransitionTo() garante que nenhuma transição inválida seja feita.
 *
 * Máquina de estados:
 *   PENDING → CONFIRMED → SHIPPED → DELIVERED
 *   PENDING → CANCELLED
 *   CONFIRMED → CANCELLED
 */
public enum OrderStatus {

    PENDING {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(CONFIRMED, CANCELLED);
        }
    },
    CONFIRMED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(SHIPPED, CANCELLED);
        }
    },
    SHIPPED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(DELIVERED);
        }
    },
    DELIVERED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of();
        }
    },
    CANCELLED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of();
        }
    };

    public abstract Set<OrderStatus> allowedTransitions();

    public boolean canTransitionTo(OrderStatus next) {
        return allowedTransitions().contains(next);
    }

    public boolean isFinal() {
        return this == DELIVERED || this == CANCELLED;
    }

    public boolean isActive() {
        return !isFinal();
    }
}