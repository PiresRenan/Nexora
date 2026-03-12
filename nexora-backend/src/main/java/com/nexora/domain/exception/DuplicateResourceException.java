package com.nexora.domain.exception;

/**
 * Lançada quando há tentativa de criar recurso duplicado.
 * Mapeia para HTTP 409 (Conflict).
 */
public non-sealed class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String resourceType, String field, String value) {
        super("%s already exists with %s: %s".formatted(resourceType, field, value));
    }
}