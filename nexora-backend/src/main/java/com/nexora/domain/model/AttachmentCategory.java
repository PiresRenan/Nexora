package com.nexora.domain.model;

/**
 * Categorias de arquivos suportados pelo sistema.
 *
 * Cada categoria define:
 *  - Se é imagem ou documento (afeta bucket de destino e validação de tipo)
 *  - Para qual entidade faz sentido (produto ou usuário)
 *
 * Limites de tamanho aplicados na camada de aplicação:
 *  - Imagens: máximo 5 MB (JPEG, PNG, WebP)
 *  - Documentos: máximo 20 MB (PDF, XML)
 */
public enum AttachmentCategory {

    // ─── Produto ──────────────────────────────────────────────────────────

    /** Foto do produto para o catálogo (exibição pública). */
    PRODUCT_IMAGE(true, EntityType.PRODUCT),

    /** Nota fiscal de compra do produto (entrada no estoque). */
    PRODUCT_INVOICE(false, EntityType.PRODUCT),

    /** Documentos técnicos, manuais, fichas de especificação. */
    PRODUCT_DOCUMENT(false, EntityType.PRODUCT),

    // ─── Usuário ──────────────────────────────────────────────────────────

    /** Foto de perfil / identificação do usuário. */
    USER_PHOTO(true, EntityType.USER),

    /** Documentos pessoais: RG, CNH, contrato de trabalho, etc. */
    USER_DOCUMENT(false, EntityType.USER);

    private final boolean image;
    private final EntityType entityType;

    AttachmentCategory(boolean image, EntityType entityType) {
        this.image      = image;
        this.entityType = entityType;
    }

    public boolean isImage()      { return image; }
    public boolean isDocument()   { return !image; }
    public EntityType entityType(){ return entityType; }
}