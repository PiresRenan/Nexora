package com.nexora.domain.port;

import java.io.InputStream;
import java.time.Duration;

/**
 * Output Port — contrato para armazenamento de arquivos binários.
 *
 * A implementação concreta (MinIO) fica na infraestrutura.
 * Um no-op bean é provido automaticamente quando MinIO não está disponível,
 * mantendo o comportamento consistente com o EventPublisher.
 *
 * Conceitos:
 *  - {@code bucket}: namespace de isolamento (ex: "nexora-images", "nexora-documents")
 *  - {@code key}: caminho único dentro do bucket (ex: "products/{id}/images/{uuid}.jpg")
 *  - Presigned URL: URL temporária de acesso direto sem autenticação
 */
public interface StoragePort {

    /**
     * Armazena um arquivo no bucket especificado.
     *
     * @param bucket      nome do bucket de destino
     * @param key         chave única do arquivo no bucket
     * @param content     stream com o conteúdo do arquivo
     * @param sizeBytes   tamanho do conteúdo em bytes
     * @param contentType MIME type (ex: "image/jpeg", "application/pdf")
     */
    void store(String bucket, String key, InputStream content, long sizeBytes, String contentType);

    /**
     * Recupera o conteúdo de um arquivo.
     * O caller é responsável por fechar o InputStream.
     */
    InputStream retrieve(String bucket, String key);

    /**
     * Gera uma URL de acesso temporário com assinatura (presigned URL).
     * Permite que o cliente faça download diretamente do object storage
     * sem passar pelo backend, reduzindo latência e consumo de memória.
     *
     * @param expiry tempo de validade da URL
     * @return URL assinada com parâmetros de autenticação embutidos
     */
    String generatePresignedUrl(String bucket, String key, Duration expiry);

    /**
     * Remove um arquivo do storage.
     * Operação idempotente — não lança exceção se o arquivo não existir.
     */
    void delete(String bucket, String key);

    /**
     * Garante que o bucket existe, criando-o se necessário.
     * Chamado na inicialização para preparar a infraestrutura.
     */
    void ensureBucketExists(String bucket);
}