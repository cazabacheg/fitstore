package com.fitstore.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/** Datos del usuario que hace la petición, leídos del JWT: {@code sub}, {@code tipo} (CLIENTE/STAFF) y {@code rol}. */
public record UsuarioActual(String id, String tipo, String rol) {

    public static UsuarioActual de(Authentication autenticacion) {
        if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt jwt) {
            return new UsuarioActual(jwt.getSubject(), jwt.getClaimAsString("tipo"), jwt.getClaimAsString("rol"));
        }
        throw new IllegalStateException("No hay un usuario autenticado con JWT");
    }
}
