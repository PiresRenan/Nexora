package com.nexora.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do object storage (MinIO/S3).
 *
 * Campos:
 *  - {@code endpoint}        URL do servidor MinIO (ex: "http://localhost:9000")
 *  - {@code accessKey}       usuário/access key (ex: "nexora")
 *  - {@code secretKey}       senha/secret key (ex: "nexora123")
 *  - {@code secure}          usar HTTPS (false para desenvolvimento local)
 *  - {@code imagesBucket}    bucket para imagens de produto e fotos de usuário
 *  - {@code documentsBucket} bucket para PDFs e documentos XML
 */
@ConfigurationProperties(prefix = "nexora.storage")
public record StorageProperties(
        String  endpoint,
        String  accessKey,
        String  secretKey,
        boolean secure,
        String  imagesBucket,
        String  documentsBucket
) {
    /** Verifica se o storage está configurado (endpoint presente e não vazio). */
    public boolean isConfigured() {
        return endpoint != null && !endpoint.isBlank();
    }
}