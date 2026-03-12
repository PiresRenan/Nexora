package com.nexora.shared.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Anotação de conveniência para injetar o UUID do usuário autenticado.
 * Uso: @CurrentUser UUID userId
 *
 * O principal no SecurityContext é o UUID do usuário (setado no JwtAuthenticationFilter).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {}