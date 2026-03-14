package com.nexora.domain.repository;

import com.nexora.domain.model.Attachment;
import com.nexora.domain.model.AttachmentCategory;
import com.nexora.domain.model.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output Port — persistência de attachments.
 */
public interface AttachmentRepository {

    Attachment save(Attachment attachment);

    Optional<Attachment> findById(UUID id);

    /** Todos os attachments de uma entidade específica. */
    List<Attachment> findByEntity(EntityType entityType, UUID entityId);

    /** Attachments de uma entidade filtrados por categoria. */
    List<Attachment> findByEntityAndCategory(EntityType entityType, UUID entityId,
                                             AttachmentCategory category);

    /** Attachment marcado como primário de uma entidade (ex: foto de perfil, imagem principal). */
    Optional<Attachment> findPrimaryByEntityAndCategory(EntityType entityType, UUID entityId,
                                                        AttachmentCategory category);

    /** Lista paginada de todos os attachments de uma entidade. */
    Page<Attachment> findByEntityPaged(EntityType entityType, UUID entityId, Pageable pageable);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}