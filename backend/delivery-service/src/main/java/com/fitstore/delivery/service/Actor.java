package com.fitstore.delivery.service;

import com.fitstore.common.security.UsuarioActual;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

/** Quien registra un avance: el staff de la sesión (id y nombre del token). */
public record Actor(UUID id, String nombre) {

    public static Actor de(Authentication autenticacion) {
        UsuarioActual usuario = UsuarioActual.de(autenticacion);
        String nombre = autenticacion.getPrincipal() instanceof Jwt jwt ? jwt.getClaimAsString("nombre") : null;
        return new Actor(UUID.fromString(usuario.id()), nombre);
    }
}
