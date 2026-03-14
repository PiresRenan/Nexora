package com.nexora.application.service;

import com.nexora.application.dto.attachment.AttachmentResponse;
import com.nexora.application.usecase.AttachmentUseCase;
import com.nexora.domain.exception.BusinessRuleException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.*;
import com.nexora.domain.port.StoragePort;
import com.nexora.domain.repository.AttachmentRepository;
import com.nexora.domain.repository.ProductRepository;
import com.nexora.domain.repository.UserRepository;
import com.nexora.infrastructure.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso de gerenciamento de arquivos do Nexora.
 *
 * Fluxo de upload:
 *  1. Validar existência da entidade dona (produto ou usuário)
 *  2. Validar tipo MIME e tamanho do arquivo
 *  3. Gerar chave única no format: {entityType}/{entityId}/{category}/{uuid}.{ext}
 *  4. Para imagens primárias: desmarcar o atual principal antes de promover o novo
 *  5. Fazer upload para o bucket correto via StoragePort
 *  6. Persistir metadados (Attachment) no banco
 *  7. Gerar presigned URL (1h para imagens, 30min para documentos sensíveis)
 *
 * Validações de arquivo:
 *  Imagens:    JPEG, PNG, WebP — máximo 5 MB
 *  Documentos: PDF, XML        — máximo 20 MB
 */
@Service
@Transactional
public class AttachmentApplicationService implements AttachmentUseCase {

    private static final Logger log = LoggerFactory.getLogger(AttachmentApplicationService.class);

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_DOCUMENT_TYPES =
            Set.of("application/pdf", "text/xml", "application/xml");

    private static final long MAX_IMAGE_SIZE    = 5L  * 1024 * 1024; //  5 MB
    private static final long MAX_DOCUMENT_SIZE = 20L * 1024 * 1024; // 20 MB

    private static final Duration IMAGE_URL_EXPIRY    = Duration.ofHours(1);
    private static final Duration DOCUMENT_URL_EXPIRY = Duration.ofMinutes(30);

    private final AttachmentRepository attachmentRepository;
    private final ProductRepository    productRepository;
    private final UserRepository       userRepository;
    private final StoragePort          storagePort;
    private final StorageProperties    storageProperties;

    public AttachmentApplicationService(AttachmentRepository attachmentRepository,
                                        ProductRepository productRepository,
                                        UserRepository userRepository,
                                        StoragePort storagePort,
                                        StorageProperties storageProperties) {
        this.attachmentRepository = attachmentRepository;
        this.productRepository    = productRepository;
        this.userRepository       = userRepository;
        this.storagePort          = storagePort;
        this.storageProperties    = storageProperties;
    }

    // ─── Produto: imagem ──────────────────────────────────────────────────

    @Override
    public AttachmentResponse uploadProductImage(UUID productId, MultipartFile file,
                                                 boolean primary, UUID uploadedBy) {
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        validateImageFile(file);

        if (primary) {
            // Desmarca a imagem principal atual antes de promover a nova
            attachmentRepository
                    .findPrimaryByEntityAndCategory(EntityType.PRODUCT, productId, AttachmentCategory.PRODUCT_IMAGE)
                    .ifPresent(existing -> {
                        existing.unsetPrimary();
                        attachmentRepository.save(existing);
                    });
        }

        log.info("Uploading product image productId={} primary={}", productId, primary);
        return upload(EntityType.PRODUCT, productId, AttachmentCategory.PRODUCT_IMAGE,
                file, primary, uploadedBy);
    }

    // ─── Produto: documento ───────────────────────────────────────────────

    @Override
    public AttachmentResponse uploadProductDocument(UUID productId, MultipartFile file,
                                                    AttachmentCategory category, UUID uploadedBy) {
        if (category != AttachmentCategory.PRODUCT_INVOICE
                && category != AttachmentCategory.PRODUCT_DOCUMENT) {
            throw new BusinessRuleException(
                    "Invalid category for product document: " + category +
                            ". Use PRODUCT_INVOICE or PRODUCT_DOCUMENT.");
        }
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        validateDocumentFile(file);

        log.info("Uploading product document productId={} category={}", productId, category);
        return upload(EntityType.PRODUCT, productId, category, file, false, uploadedBy);
    }

    // ─── Usuário: foto ────────────────────────────────────────────────────

    @Override
    public AttachmentResponse uploadUserPhoto(UUID userId, MultipartFile file, UUID uploadedBy) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        validateImageFile(file);

        // Remove e apaga do storage a foto anterior, se existir
        attachmentRepository
                .findPrimaryByEntityAndCategory(EntityType.USER, userId, AttachmentCategory.USER_PHOTO)
                .ifPresent(old -> {
                    storagePort.delete(old.getBucketName(), old.getStorageKey());
                    attachmentRepository.deleteById(old.getId());
                    log.info("Removed old user photo id={}", old.getId());
                });

        log.info("Uploading user photo userId={}", userId);
        return upload(EntityType.USER, userId, AttachmentCategory.USER_PHOTO,
                file, true, uploadedBy);
    }

    // ─── Usuário: documento ───────────────────────────────────────────────

    @Override
    public AttachmentResponse uploadUserDocument(UUID userId, MultipartFile file,
                                                 AttachmentCategory category, UUID uploadedBy) {
        if (category != AttachmentCategory.USER_DOCUMENT) {
            throw new BusinessRuleException(
                    "Invalid category for user document: " + category + ". Use USER_DOCUMENT.");
        }
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        validateDocumentFile(file);

        log.info("Uploading user document userId={} category={}", userId, category);
        return upload(EntityType.USER, userId, category, file, false, uploadedBy);
    }

    // ─── Consultas ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> findByEntity(EntityType entityType, UUID entityId) {
        return attachmentRepository.findByEntity(entityType, entityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentResponse refreshUrl(UUID attachmentId) {
        var attachment = findOrThrow(attachmentId);
        return toResponse(attachment);
    }

    // ─── Deleção ──────────────────────────────────────────────────────────

    @Override
    public void delete(UUID attachmentId, UUID requestedBy) {
        var attachment = findOrThrow(attachmentId);

        // Remove do object storage (operação idempotente)
        storagePort.delete(attachment.getBucketName(), attachment.getStorageKey());

        // Remove metadados do banco
        attachmentRepository.deleteById(attachmentId);
        log.info("Deleted attachment id={} storageKey={}", attachmentId, attachment.getStorageKey());
    }

    // ─── Núcleo de upload ─────────────────────────────────────────────────

    private AttachmentResponse upload(EntityType entityType, UUID entityId,
                                      AttachmentCategory category, MultipartFile file,
                                      boolean primary, UUID uploadedBy) {
        String bucket     = category.isImage()
                ? storageProperties.imagesBucket()
                : storageProperties.documentsBucket();
        String ext        = extractExtension(file.getOriginalFilename(), file.getContentType());
        String storageKey = buildStorageKey(entityType, entityId, category, ext);

        try {
            storagePort.store(bucket, storageKey, file.getInputStream(),
                    file.getSize(), file.getContentType());
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to read uploaded file: " + e.getMessage());
        }

        var attachment = Attachment.create(
                entityType, entityId, category,
                sanitizeFilename(file.getOriginalFilename()),
                storageKey, bucket,
                file.getContentType(), file.getSize(),
                primary, uploadedBy
        );

        var saved = attachmentRepository.save(attachment);
        log.debug("Attachment persisted id={} key={}", saved.getId(), storageKey);
        return toResponse(saved);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    private AttachmentResponse toResponse(Attachment attachment) {
        Duration expiry    = attachment.getCategory().isImage() ? IMAGE_URL_EXPIRY : DOCUMENT_URL_EXPIRY;
        Instant  expiresAt = Instant.now().plus(expiry);
        String   url       = storagePort.generatePresignedUrl(
                attachment.getBucketName(), attachment.getStorageKey(), expiry);
        return AttachmentResponse.fromDomain(attachment, url, expiresAt);
    }

    private Attachment findOrThrow(UUID id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", id));
    }

    private void validateImageFile(MultipartFile file) {
        validateNotEmpty(file);
        String ct = resolveContentType(file);
        if (!ALLOWED_IMAGE_TYPES.contains(ct)) {
            throw new BusinessRuleException(
                    "Invalid image type '%s'. Allowed: JPEG, PNG, WebP.".formatted(ct));
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessRuleException(
                    "Image too large (%d bytes). Maximum allowed: 5 MB.".formatted(file.getSize()));
        }
    }

    private void validateDocumentFile(MultipartFile file) {
        validateNotEmpty(file);
        String ct = resolveContentType(file);
        if (!ALLOWED_DOCUMENT_TYPES.contains(ct)) {
            throw new BusinessRuleException(
                    "Invalid document type '%s'. Allowed: PDF, XML.".formatted(ct));
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new BusinessRuleException(
                    "Document too large (%d bytes). Maximum allowed: 20 MB.".formatted(file.getSize()));
        }
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("File cannot be empty.");
        }
    }

    private String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null || ct.isBlank()) {
            throw new BusinessRuleException("Cannot determine file content type.");
        }
        // Normaliza tipos XML com variantes
        if (ct.contains("xml")) return "application/xml";
        return ct;
    }

    /**
     * Constrói a chave hierárquica no bucket:
     * {@code {entityType}/{entityId}/{category}/{uuid}.{ext}}
     *
     * Exemplo: {@code products/abc-123/product_image/def-456.jpg}
     */
    private String buildStorageKey(EntityType type, UUID entityId,
                                   AttachmentCategory category, String ext) {
        return "%s/%s/%s/%s.%s".formatted(
                type.name().toLowerCase(),
                entityId,
                category.name().toLowerCase(),
                UUID.randomUUID(),
                ext
        );
    }

    private String extractExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                    .toLowerCase().replaceAll("[^a-z0-9]", "");
            if (!ext.isBlank()) return ext;
        }
        // Fallback pelo content type
        return switch (contentType != null ? contentType : "") {
            case "image/jpeg"       -> "jpg";
            case "image/png"        -> "png";
            case "image/webp"       -> "webp";
            case "application/pdf"  -> "pdf";
            case "application/xml",
                 "text/xml"         -> "xml";
            default                 -> "bin";
        };
    }

    /** Remove caracteres potencialmente perigosos do nome original. */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}