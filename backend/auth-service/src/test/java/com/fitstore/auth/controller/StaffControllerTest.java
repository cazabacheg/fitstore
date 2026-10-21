package com.fitstore.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.pruebas.PruebaApi;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** HU-1-10: perfil propio del staff (CA-1-35). */
class StaffControllerTest extends PruebaApi {

    private static final String YO = "/api/v1/staff/me";
    private static final String CLAVE = "/api/v1/staff/me/password";

    @Test
    @DisplayName("CA-1-35 el staff ve su perfil con su rol y estado")
    void veSuPerfil() throws Exception {
        Staff op = crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        String token = tokenStaff("op@fitstore.pe");

        mvc.perform(get(YO).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(op.getId().toString()))
                .andExpect(jsonPath("$.data.email").value("op@fitstore.pe"))
                .andExpect(jsonPath("$.data.rol").value("OPERADOR"))
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.data.version").isNumber());
    }

    @Test
    @DisplayName("CA-1-35 al editar su nombre se actualiza; el correo y el rol no los edita el propio staff")
    void editaSuNombre() throws Exception {
        Staff op = crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        String token = tokenStaff("op@fitstore.pe");

        mvc.perform(put(YO).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombres", "Marco Antonio", "apellidos", "Rojas Díaz",
                                "rol", "ADMIN", "email", "otro@fitstore.pe"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombres").value("Marco Antonio"))
                .andExpect(jsonPath("$.data.apellidos").value("Rojas Díaz"))
                .andExpect(jsonPath("$.data.rol").value("OPERADOR"))
                .andExpect(jsonPath("$.data.email").value("op@fitstore.pe"));

        Map<String, Object> fila = jdbc.queryForMap("SELECT nombres, rol, email FROM staff WHERE id = ?::uuid", op.getId().toString());
        assertThat(fila).containsEntry("nombres", "Marco Antonio").containsEntry("rol", "OPERADOR")
                .containsEntry("email", "op@fitstore.pe");
    }

    @Test
    @DisplayName("CA-1-35 un nombre demasiado corto responde 400 con el campo")
    void nombreInvalido() throws Exception {
        crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        String token = tokenStaff("op@fitstore.pe");

        mvc.perform(put(YO).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombres", "M", "apellidos", "Rojas"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nombres"));
    }

    @Test
    @DisplayName("CA-1-35 con la contraseña actual correcta se cambia y se cierran las otras sesiones del staff")
    void cambiaLaPassword() throws Exception {
        crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        Cookie otroDispositivo = mvc.perform(postJson("/api/v1/auth/staff/login", Map.of("email", "op@fitstore.pe", "password", PASSWORD)))
                .andReturn().getResponse().getCookie("fs_refresh");
        String token = tokenStaff("op@fitstore.pe");

        mvc.perform(put(CLAVE).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("passwordActual", PASSWORD, "nuevaPassword", "NuevaClave2027"))))
                .andExpect(status().isOk());

        mvc.perform(postJson("/api/v1/auth/staff/login", Map.of("email", "op@fitstore.pe", "password", "NuevaClave2027")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").cookie(otroDispositivo)).andExpect(status().isUnauthorized());
        assertThat(json.readTree(eventos("PasswordCambiado").get(0).get("payload").toString()).path("tipoCuenta").asString())
                .isEqualTo("STAFF");
    }

    @Test
    @DisplayName("CA-1-35 una contraseña actual incorrecta responde 400 en passwordActual")
    void passwordActualIncorrecta() throws Exception {
        crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        String token = tokenStaff("op@fitstore.pe");

        mvc.perform(put(CLAVE).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("passwordActual", "Incorrecta1", "nuevaPassword", "NuevaClave2027"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("passwordActual"));
    }

    @Test
    @DisplayName("CA-1-35 los endpoints propios del staff rechazan un token de cliente (403) y la falta de token (401)")
    void soloElStaff() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String cliente = tokenCliente("ana@correo.com");

        mvc.perform(get(YO)).andExpect(status().isUnauthorized());
        mvc.perform(get(YO).header("Authorization", bearer(cliente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        mvc.perform(put(CLAVE).header("Authorization", bearer(cliente)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("passwordActual", PASSWORD, "nuevaPassword", "NuevaClave2027"))))
                .andExpect(status().isForbidden());
    }
}
