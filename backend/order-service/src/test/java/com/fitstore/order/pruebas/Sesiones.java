package com.fitstore.order.pruebas;

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
        return staff(ADMIN_ID, "ADMIN", "Ana Admin");
    }

    public static RequestPostProcessor operador() {
        return staff(OPERADOR_ID, "OPERADOR", "Omar Operador");
    }

    public static RequestPostProcessor cliente(UUID id) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("tipo", "CLIENTE").claim("rol", "CLIENTE")
                .claim("nombre", "Cliente Prueba"))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"));
    }

    private static RequestPostProcessor staff(String id, String rol, String nombre) {
        return jwt().jwt(j -> j.subject(id).claim("tipo", "STAFF").claim("rol", rol).claim("nombre", nombre))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
