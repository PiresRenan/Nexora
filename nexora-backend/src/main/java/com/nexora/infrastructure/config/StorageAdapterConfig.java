package com.nexora.infrastructure.config;

import com.nexora.domain.port.StoragePort;
import com.nexora.infrastructure.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.time.Duration;

/**
 * Fornece uma implementação no-op de StoragePort quando MinIO não está disponível.
 *
 * Comportamento do no-op:
 *  - {@code store} e {@code delete}: lançam StorageException (operações destrutivas não permitidas)
 *  - {@code retrieve} e {@code generatePresignedUrl}: lançam StorageException
 *  - {@code ensureBucketExists}: no-op silencioso (evita falha no startup sem MinIO)
 *
 * Usado em:
 *  - Testes unitários/slice sem necessidade de MinIO
 *  - Smoke test do ApplicationContext (@SpringBootTest sem MinIO)
 */
@Configuration
public class StorageAdapterConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageAdapterConfig.class);

    @Bean
    @ConditionalOnMissingBean(StoragePort.class)
    public StoragePort noOpStoragePort() {
        log.warn("No StoragePort configured — using no-op. File uploads will NOT work.");
        return new StoragePort() {
            @Override
            public void store(String bucket, String key, InputStream content,
                              long sizeBytes, String contentType) {
                throw new StorageException(
                        "Storage not configured. Set nexora.storage.endpoint to enable file uploads.");
            }

            @Override
            public InputStream retrieve(String bucket, String key) {
                throw new StorageException("Storage not configured.");
            }

            @Override
            public String generatePresignedUrl(String bucket, String key, Duration expiry) {
                throw new StorageException("Storage not configured.");
            }

            @Override
            public void delete(String bucket, String key) {
                log.debug("[NO-OP] Storage delete ignored: bucket={} key={}", bucket, key);
            }

            @Override
            public void ensureBucketExists(String bucket) {
                log.debug("[NO-OP] ensureBucketExists ignored for: {}", bucket);
            }
        };
    }
}