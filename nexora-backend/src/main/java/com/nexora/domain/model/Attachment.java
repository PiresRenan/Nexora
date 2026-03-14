package com.nexora.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate Root: Attachment (arquivo anexado a uma entidade do sistema).
 *
 * Representa os metadados de um arquivo armazenado no object storage (MinIO/S3).
 * O conteúdo binário NUNCA é armazenado no banco — apenas a referência (storageKey + bucket).
 *
 * Invariantes:
 *  - Uma entidade pode ter apenas um attachment com {@code primary = true} por categoria de imagem
 *    (controlado no ApplicationService)
 *  - O {@code storageKey} é imutável após a criação — identifica o arquivo no object storage
 *  - {@code sizeBytes} deve ser positivo
 */
public class Attachment {

    private final UUID               id;
    private final EntityType         entityType;
    private final UUID               entityId;
    private final AttachmentCategory category;
    private final String             originalFilename;
    private final String             storageKey;    // chave no bucket MinIO (imutável)
    private final String             bucketName;
    private final String             contentType;   // MIME type
    private final long               sizeBytes;
    private       boolean            primary;       // imagem/foto principal da entidade
    private final UUID               uploadedBy;
    private final Instant            uploadedAt;

    // ─── Factory Methods ───────────────────────────────────────────────────

    public static Attachment create(
            EntityType entityType, UUID entityId, AttachmentCategory category,
            String originalFilename, String storageKey, String bucketName,
            String contentType, long sizeBytes, boolean primary, UUID uploadedBy
    ) {
        return new Attachment(
                UUID.randomUUID(), entityType, entityId, category,
                originalFilename, storageKey, bucketName, contentType,
                sizeBytes, primary, uploadedBy, Instant.now()
        );
    }

    public static Attachment reconstitute(
            UUID id, EntityType entityType, UUID entityId, AttachmentCategory category,
            String originalFilename, String storageKey, String bucketName,
            String contentType, long sizeBytes, boolean primary,
            UUID uploadedBy, Instant uploadedAt
    ) {
        return new Attachment(id, entityType, entityId, category, originalFilename,
                storageKey, bucketName, contentType, sizeBytes, primary, uploadedBy, uploadedAt);
    }

    private Attachment(
            UUID id, EntityType entityType, UUID entityId, AttachmentCategory category,
            String originalFilename, String storageKey, String bucketName,
            String contentType, long sizeBytes, boolean primary,
            UUID uploadedBy, Instant uploadedAt
    ) {
        if (sizeBytes <= 0) throw new IllegalArgumentException("File size must be positive");
        this.id               = Objects.requireNonNull(id);
        this.entityType       = Objects.requireNonNull(entityType);
        this.entityId         = Objects.requireNonNull(entityId);
        this.category         = Objects.requireNonNull(category);
        this.originalFilename = requireNonBlank(originalFilename, "originalFilename");
        this.storageKey       = requireNonBlank(storageKey, "storageKey");
        this.bucketName       = requireNonBlank(bucketName, "bucketName");
        this.contentType      = requireNonBlank(contentType, "contentType");
        this.sizeBytes        = sizeBytes;
        this.primary          = primary;
        this.uploadedBy       = uploadedBy;
        this.uploadedAt       = Objects.requireNonNull(uploadedAt);
    }

    // ─── Comportamentos ────────────────────────────────────────────────────

    /** Promove este attachment como o principal da entidade. */
    public void markAsPrimary() {
        this.primary = true;
    }

    /** Remove o status de principal (ao ser substituído por outro). */
    public void unsetPrimary() {
        this.primary = false;
    }

    // ─── Getters ───────────────────────────────────────────────────────────

    public UUID               getId()               { return id; }
    public EntityType         getEntityType()       { return entityType; }
    public UUID               getEntityId()         { return entityId; }
    public AttachmentCategory getCategory()         { return category; }
    public String             getOriginalFilename() { return originalFilename; }
    public String             getStorageKey()       { return storageKey; }
    public String             getBucketName()       { return bucketName; }
    public String             getContentType()      { return contentType; }
    public long               getSizeBytes()        { return sizeBytes; }
    public boolean            isPrimary()           { return primary; }
    public UUID               getUploadedBy()       { return uploadedBy; }
    public Instant            getUploadedAt()       { return uploadedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attachment a)) return false;
        return Objects.equals(id, a.id);
    }

    @Override public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Attachment{id=%s, category=%s, entity=%s/%s, primary=%s}"
                .formatted(id, category, entityType, entityId, primary);
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return v;
    }
}