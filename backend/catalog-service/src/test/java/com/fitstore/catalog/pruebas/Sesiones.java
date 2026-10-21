package com.fitstore.catalog.pruebas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Sesiones de prueba (JWT simulado) con la autoridad {@code ROLE_*} que arma el convertidor de fitstore-common. */
public final class Sesiones {

    public static final String ADMIN_ID = "5e738102-a490-469f-b0f6-5f1fc2dbf70c";
    public static final String OPERADOR_ID = "9b0f8f55-35ec-4a0d-9c1b-0c1a3a0a1111";
    public static final String CLIENTE_ID = "35a11d4f-054a-44a5-8ac4-cf0ed4020f8c";

    private Sesiones() {
    }

    public static RequestPostProcessor admin() {
        return sesion(ADMIN_ID, "STAFF", "ADMIN");
    }

    public static RequestPostProcessor operador() {
        return sesion(OPERADOR_ID, "STAFF", "OPERADOR");
    }

    /** Un cliente: tiene JWT válido pero ningún rol de staff. */
    public static RequestPostProcessor cliente() {
        return jwt().jwt(j -> j.subject(CLIENTE_ID).claim("tipo", "CLIENTE"));
    }

    private static RequestPostProcessor sesion(String id, String tipo, String rol) {
        return jwt()
                .jwt(j -> j.subject(id).claim("tipo", tipo).claim("rol", rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
