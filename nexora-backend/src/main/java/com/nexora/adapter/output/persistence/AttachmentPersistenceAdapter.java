package com.nexora.adapter.output.persistence;

import com.nexora.adapter.output.persistence.jpa.AttachmentJpaRepository;
import com.nexora.domain.model.Attachment;
import com.nexora.domain.model.AttachmentCategory;
import com.nexora.domain.model.EntityType;
import com.nexora.domain.repository.AttachmentRepository;
import com.nexora.infrastructure.persistence.entity.AttachmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AttachmentPersistenceAdapter implements AttachmentRepository {

    private final AttachmentJpaRepository jpa;

    public AttachmentPersistenceAdapter(AttachmentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Attachment save(Attachment attachment) {
        return jpa.save(AttachmentEntity.fromDomain(attachment)).toDomain();
    }

    @Override
    public Optional<Attachment> findById(UUID id) {
        return jpa.findById(id).map(AttachmentEntity::toDomain);
    }

    @Override
    public List<Attachment> findByEntity(EntityType entityType, UUID entityId) {
        return jpa.findByEntityTypeAndEntityId(entityType, entityId)
                .stream().map(AttachmentEntity::toDomain).toList();
    }

    @Override
    public List<Attachment> findByEntityAndCategory(EntityType entityType, UUID entityId,
                                                    AttachmentCategory category) {
        return jpa.findByEntityTypeAndEntityIdAndCategory(entityType, entityId, category)
                .stream().map(AttachmentEntity::toDomain).toList();
    }

    @Override
    public Optional<Attachment> findPrimaryByEntityAndCategory(EntityType entityType,
                                                               UUID entityId,
                                                               AttachmentCategory category) {
        return jpa.findByEntityTypeAndEntityIdAndCategoryAndPrimaryTrue(entityType, entityId, category)
                .map(AttachmentEntity::toDomain);
    }

    @Override
    public Page<Attachment> findByEntityPaged(EntityType entityType, UUID entityId,
                                              Pageable pageable) {
        return jpa.findByEntityTypeAndEntityId(entityType, entityId, pageable)
                .map(AttachmentEntity::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }
}