package com.fitstore.payment.pruebas;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.UUID;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Sesiones de prueba (JWT simulado) con la autoridad {@code ROLE_*} que arma el convertidor de fitstore-common. */
public final class Sesiones {

    public static final String ADMIN_ID = "5e738102-a490-469f-b0f6-5f1fc2dbf70c";
    public static final String OPERADOR_ID = "9b0f8f55-35ec-4a0d-9c1b-0c1a3a0a1111";

    private Sesiones() {
    }

    public static RequestPostProcessor admin() {
        return sesion(ADMIN_ID, "STAFF", "ADMIN");
    }

    public static RequestPostProcessor operador() {
        return sesion(OPERADOR_ID, "STAFF", "OPERADOR");
    }

    /** Una clienta: JWT válido con {@code tipo = CLIENTE} y ningún rol de staff. */
    public static RequestPostProcessor cliente(UUID clienteId) {
        return jwt().jwt(j -> j.subject(clienteId.toString()).claim("tipo", "CLIENTE"));
    }

    private static RequestPostProcessor sesion(String id, String tipo, String rol) {
        return jwt()
                .jwt(j -> j.subject(id).claim("tipo", tipo).claim("rol", rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
