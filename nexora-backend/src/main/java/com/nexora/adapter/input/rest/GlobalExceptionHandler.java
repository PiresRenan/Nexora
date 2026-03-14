package com.nexora.adapter.input.rest;

import com.nexora.domain.exception.*;
import com.nexora.infrastructure.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        p.setType(URI.create("https://nexora.com/errors/resource-not-found"));
        p.setTitle("Resource Not Found");
        return p;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        log.warn("Duplicate: {}", ex.getMessage());
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        p.setType(URI.create("https://nexora.com/errors/duplicate-resource"));
        p.setTitle("Duplicate Resource");
        return p;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusiness(BusinessRuleException ex) {
        log.warn("Business rule: {}", ex.getMessage());
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        p.setType(URI.create("https://nexora.com/errors/business-rule-violation"));
        p.setTitle("Business Rule Violation");
        return p;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "You don't have permission to perform this action.");
        p.setType(URI.create("https://nexora.com/errors/access-denied"));
        p.setTitle("Access Denied");
        return p;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Authentication failed. Check your credentials.");
        p.setType(URI.create("https://nexora.com/errors/authentication-failed"));
        p.setTitle("Authentication Failed");
        return p;
    }

    /**
     * Arquivo maior que o limite configurado em spring.servlet.multipart.max-file-size.
     * Retorna 413 Payload Too Large com detalhe claro.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "File size exceeds the maximum allowed limit (images: 5 MB, documents: 20 MB).");
        p.setType(URI.create("https://nexora.com/errors/file-too-large"));
        p.setTitle("File Too Large");
        return p;
    }

    /**
     * Falha no object storage (MinIO indisponível, bucket inexistente, etc.).
     * Retorna 503 Service Unavailable — permite retry por parte do cliente.
     */
    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorage(StorageException ex) {
        log.error("Storage error: {}", ex.getMessage(), ex);
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "File storage is temporarily unavailable. Please try again later.");
        p.setType(URI.create("https://nexora.com/errors/storage-unavailable"));
        p.setTitle("Storage Unavailable");
        return p;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request
    ) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing));
        log.warn("Validation failed: {}", fieldErrors);
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request validation failed. Check the 'errors' field for details.");
        p.setType(URI.create("https://nexora.com/errors/validation-error"));
        p.setTitle("Validation Error");
        p.setProperty("errors", fieldErrors);
        return ResponseEntity.badRequest().body(p);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        p.setTitle("Invalid Argument");
        return p;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        var p = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        p.setTitle("Internal Server Error");
        return p;
    }
}