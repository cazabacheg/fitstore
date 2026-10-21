package com.fitstore.notification.pruebas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Sesiones de staff para las pruebas de API (el claim {@code rol} lo pone auth en el token real). */
public final class Sesiones {

    public static final String ID_ADMIN = "11111111-1111-4111-8111-111111111111";
    public static final String ID_OPERADOR = "5e738102-a490-469f-b0f6-5f1fc2dbf70c";

    private Sesiones() {
    }

    public static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                .jwt(j -> j.subject(ID_ADMIN).claim("tipo", "STAFF").claim("rol", "ADMIN"));
    }

    public static RequestPostProcessor operador() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_OPERADOR"))
                .jwt(j -> j.subject(ID_OPERADOR).claim("tipo", "STAFF").claim("rol", "OPERADOR"));
    }
}
