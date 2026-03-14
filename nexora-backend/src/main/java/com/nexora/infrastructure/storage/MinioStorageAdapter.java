package com.nexora.infrastructure.storage;

import com.nexora.domain.port.StoragePort;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Implementação do StoragePort usando MinIO (S3-compatível).
 *
 * Comportamento:
 *  - Upload: usa PutObjectArgs com content-type explícito
 *  - Download: retorna InputStream (caller deve fechar)
 *  - Presigned URL: válida pelo período informado (máx. 7 dias pelo protocolo S3)
 *  - Delete: operação idempotente — ignora "not found"
 *  - Buckets: criados automaticamente no startup se não existirem
 *
 * Este bean só é criado quando {@link MinioClient} está disponível no contexto,
 * garantindo que testes sem MinIO funcionem via fallback no-op.
 */
@Component
@ConditionalOnBean(MinioClient.class)
public class MinioStorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageAdapter.class);

    private final MinioClient minioClient;

    public MinioStorageAdapter(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public void store(String bucket, String key, InputStream content,
                      long sizeBytes, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(content, sizeBytes, -1)
                            .contentType(contentType)
                            .build()
            );
            log.debug("Stored object bucket={} key={} size={}", bucket, key, sizeBytes);
        } catch (Exception e) {
            throw new StorageException("Failed to store file '%s' in bucket '%s': %s"
                    .formatted(key, bucket, e.getMessage()), e);
        }
    }

    @Override
    public InputStream retrieve(String bucket, String key) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to retrieve file '%s' from bucket '%s': %s"
                    .formatted(key, bucket, e.getMessage()), e);
        }
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, Duration expiry) {
        try {
            int expirySeconds = (int) Math.min(expiry.getSeconds(), 7 * 24 * 3600L);
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to generate presigned URL for '%s': %s"
                    .formatted(key, e.getMessage()), e);
        }
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
            log.debug("Deleted object bucket={} key={}", bucket, key);
        } catch (Exception e) {
            // Operação idempotente: log mas não lança exceção
            log.warn("Failed to delete object bucket={} key={}: {}", bucket, key, e.getMessage());
        }
    }

    @Override
    public void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            throw new StorageException("Failed to ensure bucket '%s' exists: %s"
                    .formatted(bucket, e.getMessage()), e);
        }
    }
}