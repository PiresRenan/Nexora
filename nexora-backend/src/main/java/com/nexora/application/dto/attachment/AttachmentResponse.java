package com.nexora.application.dto.attachment;

import com.nexora.domain.model.Attachment;
import com.nexora.domain.model.AttachmentCategory;
import com.nexora.domain.model.EntityType;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta para um arquivo anexado.
 *
 * O campo {@code url} é uma presigned URL com validade de 1 hora.
 * Após expirar, obtenha uma nova URL via GET /api/v1/attachments/{id}/url.
 *
 * O campo {@code urlExpiresAt} indica quando a URL expira para facilitar
 * o cache no cliente.
 */
public record AttachmentResponse(
        UUID               id,
        EntityType         entityType,
        UUID               entityId,
        AttachmentCategory category,
        String             originalFilename,
        String             contentType,
        long               sizeBytes,
        boolean            primary,
        UUID               uploadedBy,
        Instant            uploadedAt,
        String             url,           // presigned URL de acesso direto
        Instant            urlExpiresAt   // validade da presigned URL
) {
    public static AttachmentResponse fromDomain(Attachment a, String url, Instant urlExpiresAt) {
        return new AttachmentResponse(
                a.getId(), a.getEntityType(), a.getEntityId(),
                a.getCategory(), a.getOriginalFilename(),
                a.getContentType(), a.getSizeBytes(),
                a.isPrimary(), a.getUploadedBy(), a.getUploadedAt(),
                url, urlExpiresAt
        );
    }
}