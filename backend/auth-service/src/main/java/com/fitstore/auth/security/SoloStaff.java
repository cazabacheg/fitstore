package com.fitstore.auth.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/** Cualquier miembro del staff (OPERADOR o ADMIN, que hereda de OPERADOR) con el claim {@code tipo = STAFF}. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole('OPERADOR') and authentication.token.claims['tipo'] == 'STAFF'")
public @interface SoloStaff {
}
