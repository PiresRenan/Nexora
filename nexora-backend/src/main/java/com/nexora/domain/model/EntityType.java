package com.nexora.domain.model;

/**
 * Tipo de entidade dona do attachment.
 * Usada na associação polimórfica de {@link Attachment} — um attachment
 * pertence a um produto OU a um usuário, identificado por (entityType + entityId).
 */
public enum EntityType {
    PRODUCT,
    USER
}