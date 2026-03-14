package com.nexora.infrastructure.persistence.entity;

import com.nexora.domain.model.Attachment;
import com.nexora.domain.model.AttachmentCategory;
import com.nexora.domain.model.EntityType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA de metadados de arquivo.
 * O conteúdo binário NUNCA é armazenado aqui — apenas a referência ao MinIO.
 */
@Entity
@Table(
        name = "attachments",
        indexes = {
                @Index(name = "idx_attachments_entity",   columnList = "entity_type, entity_id"),
                @Index(name = "idx_attachments_category", columnList = "entity_type, entity_id, category"),
                @Index(name = "idx_attachments_primary",  columnList = "entity_type, entity_id, category, primary_flag"),
                @Index(name = "idx_attachments_uploaded", columnList = "uploaded_at")
        }
)
public class AttachmentEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttachmentCategory category;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "primary_flag", nullable = false)
    private boolean primary;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected AttachmentEntity() {}

    public static AttachmentEntity fromDomain(Attachment a) {
        var e = new AttachmentEntity();
        e.id               = a.getId();
        e.entityType       = a.getEntityType();
        e.entityId         = a.getEntityId();
        e.category         = a.getCategory();
        e.originalFilename = a.getOriginalFilename();
        e.storageKey       = a.getStorageKey();
        e.bucketName       = a.getBucketName();
        e.contentType      = a.getContentType();
        e.sizeBytes        = a.getSizeBytes();
        e.primary          = a.isPrimary();
        e.uploadedBy       = a.getUploadedBy();
        e.uploadedAt       = a.getUploadedAt();
        return e;
    }

    public Attachment toDomain() {
        return Attachment.reconstitute(
                id, entityType, entityId, category,
                originalFilename, storageKey, bucketName,
                contentType, sizeBytes, primary,
                uploadedBy, uploadedAt
        );
    }

    public UUID               getId()         { return id; }
    public EntityType         getEntityType() { return entityType; }
    public UUID               getEntityId()   { return entityId; }
    public AttachmentCategory getCategory()   { return category; }
    public boolean            isPrimary()     { return primary; }
}