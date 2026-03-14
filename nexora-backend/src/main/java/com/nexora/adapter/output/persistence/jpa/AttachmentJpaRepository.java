package com.nexora.adapter.output.persistence.jpa;

import com.nexora.domain.model.AttachmentCategory;
import com.nexora.domain.model.EntityType;
import com.nexora.infrastructure.persistence.entity.AttachmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttachmentJpaRepository extends JpaRepository<AttachmentEntity, UUID> {

    List<AttachmentEntity> findByEntityTypeAndEntityId(EntityType entityType, UUID entityId);

    List<AttachmentEntity> findByEntityTypeAndEntityIdAndCategory(
            EntityType entityType, UUID entityId, AttachmentCategory category);

    Optional<AttachmentEntity> findByEntityTypeAndEntityIdAndCategoryAndPrimaryTrue(
            EntityType entityType, UUID entityId, AttachmentCategory category);

    Page<AttachmentEntity> findByEntityTypeAndEntityId(
            EntityType entityType, UUID entityId, Pageable pageable);
}