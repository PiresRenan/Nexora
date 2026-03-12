package com.nexora.domain.exception;

import java.util.UUID;

/**
 * Lançada quando um recurso não é encontrado.
 * Mapeia para HTTP 404.
 */
public non-sealed class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resourceType, UUID id) {
        super("%s not found with id: %s".formatted(resourceType, id));
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super("%s not found: %s".formatted(resourceType, identifier));
    }
}