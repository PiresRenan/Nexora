package com.nexora.infrastructure.storage;

/**
 * Exceção lançada por falhas no object storage (MinIO/S3).
 * Mapeada pelo GlobalExceptionHandler para HTTP 503 Service Unavailable.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}