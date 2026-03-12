package com.nexora.domain.exception;

/**
 * Exceção raiz do domínio.
 * Melhoria: classe selada (sealed) para garantir que apenas exceções
 * conhecidas e intencionais sejam lançadas pelo domínio.
 * Todas as exceções de domínio herdam desta — o GlobalExceptionHandler
 * pode tratar de forma uniforme.
 */
public sealed class DomainException extends RuntimeException
        permits ResourceNotFoundException, BusinessRuleException, DuplicateResourceException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}