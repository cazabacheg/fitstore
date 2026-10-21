package com.fitstore.auth.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/** Solo una sesión de cliente: rol {@code CLIENTE} y claim {@code tipo = CLIENTE} (micro 8). */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('CLIENTE') and authentication.token.claims['tipo'] == 'CLIENTE'")
public @interface SoloCliente {
}
