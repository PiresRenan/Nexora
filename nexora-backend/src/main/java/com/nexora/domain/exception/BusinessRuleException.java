package com.nexora.domain.exception;

/**
 * Lançada quando uma regra de negócio é violada.
 * Mapeia para HTTP 422 (Unprocessable Entity).
 */
public non-sealed class BusinessRuleException extends DomainException {

    public BusinessRuleException(String message) {
        super(message);
    }
}