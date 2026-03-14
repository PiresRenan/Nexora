-- V11__create_attachments_table.sql
-- Tabela de metadados de arquivos (imagens e documentos).
-- O conteúdo binário é armazenado no MinIO — aqui ficam apenas os metadados e
-- a chave de referência (storage_key + bucket_name).

CREATE TABLE attachments (
                             id                UUID         NOT NULL,
                             entity_type       VARCHAR(20)  NOT NULL,   -- PRODUCT | USER
                             entity_id         UUID         NOT NULL,
                             category          VARCHAR(30)  NOT NULL,   -- PRODUCT_IMAGE | PRODUCT_INVOICE | ...
                             original_filename VARCHAR(255) NOT NULL,
                             storage_key       VARCHAR(512) NOT NULL,   -- caminho único no bucket MinIO
                             bucket_name       VARCHAR(100) NOT NULL,
                             content_type      VARCHAR(100) NOT NULL,   -- MIME type
                             size_bytes        BIGINT       NOT NULL CHECK (size_bytes > 0),
                             primary_flag      BOOLEAN      NOT NULL DEFAULT FALSE,
                             uploaded_by       UUID         REFERENCES users(id) ON DELETE SET NULL,
                             uploaded_at       TIMESTAMP    NOT NULL DEFAULT NOW(),

                             CONSTRAINT pk_attachments        PRIMARY KEY (id),
                             CONSTRAINT uq_attachments_key    UNIQUE (bucket_name, storage_key),
                             CONSTRAINT chk_entity_type       CHECK (entity_type IN ('PRODUCT', 'USER')),
                             CONSTRAINT chk_category          CHECK (category IN (
                                                                                  'PRODUCT_IMAGE', 'PRODUCT_INVOICE', 'PRODUCT_DOCUMENT',
                                                                                  'USER_PHOTO', 'USER_DOCUMENT'
                                 ))
);

-- Índice principal: busca de todos os arquivos de uma entidade
CREATE INDEX idx_attachments_entity
    ON attachments (entity_type, entity_id);

-- Índice para busca por entidade + categoria (mais comum)
CREATE INDEX idx_attachments_entity_category
    ON attachments (entity_type, entity_id, category);

-- Índice parcial para localizar o arquivo "principal" rapidamente
-- Um produto/usuário raramente tem mais de 1 primário por categoria
CREATE UNIQUE INDEX idx_attachments_primary_unique
    ON attachments (entity_type, entity_id, category)
    WHERE primary_flag = TRUE;

-- Índice para ordenação cronológica e relatórios
CREATE INDEX idx_attachments_uploaded
    ON attachments (uploaded_at DESC);

-- Índice para rastrear uploads por usuário
CREATE INDEX idx_attachments_uploader
    ON attachments (uploaded_by)
    WHERE uploaded_by IS NOT NULL;

COMMENT ON TABLE attachments IS
    'Metadados de arquivos armazenados no MinIO. Nunca armazena conteúdo binário.';
COMMENT ON COLUMN attachments.storage_key IS
    'Caminho único no bucket: {entityType}/{entityId}/{category}/{uuid}.{ext}';
COMMENT ON COLUMN attachments.primary_flag IS
    'TRUE = arquivo principal da categoria (ex: foto de perfil, imagem principal do produto).
     Garantido único por (entity_type, entity_id, category) via índice parcial.';
COMMENT ON COLUMN attachments.uploaded_by IS
    'Usuário que fez o upload. NULL se o usuário foi removido (ON DELETE SET NULL).';