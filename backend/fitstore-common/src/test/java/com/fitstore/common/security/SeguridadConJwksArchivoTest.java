package com.fitstore.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Resource server de fitstore-common con el JWKS de prueba leído de un archivo (SPEC-0a I2). */
@WebMvcTest(controllers = SeguridadConJwksArchivoTest.Controlador.class)
@ImportAutoConfiguration(FitstoreSecurityAutoConfiguration.class)
@Import(SeguridadConJwksArchivoTest.Controlador.class)
class SeguridadConJwksArchivoTest {

    static final TokensDePrueba EMISOR = new TokensDePrueba("kid-prueba");
    static final TokensDePrueba OTRO_EMISOR = new TokensDePrueba("kid-prueba"); // mismo kid, otra clave

    @SpringBootConfiguration
    static class Aplicacion { }

    @RestController
    static class Controlador {
        @GetMapping("/actuator/health")
        String salud() { return "UP"; }

        @GetMapping("/v3/api-docs")
        String docs() { return "{}"; }

        @GetMapping("/api/v1/yo")
        String yo(Authentication autenticacion) {
            UsuarioActual u = UsuarioActual.de(autenticacion);
            return u.id() + "|" + u.tipo() + "|" + u.rol();
        }

        @GetMapping("/api/v1/operacion")
        @PreAuthorize("hasRole('OPERADOR')")
        String operacion() { return "ok"; }

        @GetMapping("/api/v1/solo-admin")
        @PreAuthorize("hasRole('ADMIN')")
        String soloAdmin() { return "ok"; }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("fitstore.security.jwks-archivo", () -> EMISOR.escribirJwks().toString());
    }

    @Autowired MockMvc mvc;

    private static String bearer(String rol) {
        return "Bearer " + EMISOR.token("42", rol.equals("CLIENTE") ? "CLIENTE" : "STAFF", rol, Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("I3 solo /actuator/health y la documentación OpenAPI están abiertas")
    void rutasAbiertas() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("I3 sin token todo lo demás responde 401 con formato ApiError")
    void sinToken() throws Exception {
        mvc.perform(get("/api/v1/yo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("un token firmado con otra clave responde 401")
    void firmaInvalida() throws Exception {
        String falso = OTRO_EMISOR.token("42", "STAFF", "ADMIN", Duration.ofMinutes(15));

        mvc.perform(get("/api/v1/yo").header("Authorization", "Bearer " + falso))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("un token vencido responde 401")
    void vencido() throws Exception {
        String vencido = EMISOR.token("42", "STAFF", "ADMIN", Duration.ofMinutes(-5));

        mvc.perform(get("/api/v1/yo").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("UsuarioActual lee sub, tipo y rol del token")
    void usuarioActual() throws Exception {
        mvc.perform(get("/api/v1/yo").header("Authorization", bearer("OPERADOR")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("42|STAFF|OPERADOR"));
    }

    @Test
    @DisplayName("RoleHierarchy: ADMIN puede lo que puede OPERADOR")
    void adminHeredaOperador() throws Exception {
        mvc.perform(get("/api/v1/operacion").header("Authorization", bearer("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/operacion").header("Authorization", bearer("OPERADOR"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("RoleHierarchy: OPERADOR no puede lo de ADMIN y CLIENTE no entra a operaciones de staff")
    void rolInsuficiente() throws Exception {
        mvc.perform(get("/api/v1/solo-admin").header("Authorization", bearer("OPERADOR")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        mvc.perform(get("/api/v1/operacion").header("Authorization", bearer("CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }
}
