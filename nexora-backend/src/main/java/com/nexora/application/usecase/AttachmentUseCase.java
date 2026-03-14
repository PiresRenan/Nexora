package com.nexora.application.usecase;

import com.nexora.application.dto.attachment.AttachmentResponse;
import com.nexora.domain.model.AttachmentCategory;
import com.nexora.domain.model.EntityType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Input Port — gerenciamento de arquivos anexados a entidades do sistema.
 *
 * Contratos por categoria:
 *  - PRODUCT_IMAGE:    JPEG / PNG / WebP, máx. 5 MB, acesso público nas GET
 *  - PRODUCT_INVOICE:  PDF / XML, máx. 20 MB, acesso restrito a SELLER+
 *  - PRODUCT_DOCUMENT: PDF / XML, máx. 20 MB, acesso restrito a SELLER+
 *  - USER_PHOTO:       JPEG / PNG / WebP, máx. 5 MB, apenas 1 por usuário (substitui anterior)
 *  - USER_DOCUMENT:    PDF / XML, máx. 20 MB, acesso restrito a MANAGER+
 */
public interface AttachmentUseCase {

    /**
     * Faz upload de uma imagem de produto.
     * Se {@code primary = true}, substitui a imagem principal anterior.
     */
    AttachmentResponse uploadProductImage(UUID productId, MultipartFile file,
                                          boolean primary, UUID uploadedBy);

    /**
     * Faz upload de um documento de produto (nota fiscal, manual, etc.).
     * {@code category} deve ser PRODUCT_INVOICE ou PRODUCT_DOCUMENT.
     */
    AttachmentResponse uploadProductDocument(UUID productId, MultipartFile file,
                                             AttachmentCategory category, UUID uploadedBy);

    /**
     * Faz upload da foto de perfil de um usuário.
     * Substitui a foto anterior se existir (a antiga é removida do storage).
     */
    AttachmentResponse uploadUserPhoto(UUID userId, MultipartFile file, UUID uploadedBy);

    /**
     * Faz upload de documento pessoal de um usuário (RG, CNH, contrato, etc.).
     * {@code category} deve ser USER_DOCUMENT.
     */
    AttachmentResponse uploadUserDocument(UUID userId, MultipartFile file,
                                          AttachmentCategory category, UUID uploadedBy);

    /**
     * Lista todos os attachments de uma entidade.
     * Imagens de produto são públicas; demais categorias exigem papel adequado.
     */
    List<AttachmentResponse> findByEntity(EntityType entityType, UUID entityId);

    /**
     * Gera uma nova presigned URL para download direto do arquivo.
     * Útil quando a URL anterior expirou ou para compartilhamento temporário.
     */
    AttachmentResponse refreshUrl(UUID attachmentId);

    /**
     * Remove um attachment do sistema e do object storage.
     * Imagens: SELLER+ pode remover de qualquer produto.
     * Documentos de usuário: apenas MANAGER+ pode remover.
     */
    void delete(UUID attachmentId, UUID requestedBy);
}