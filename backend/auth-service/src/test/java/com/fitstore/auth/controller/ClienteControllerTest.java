package com.fitstore.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.pruebas.PruebaApi;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** HU-1-05: perfil y contraseña del cliente (CA-1-15 y CA-1-16). */
class ClienteControllerTest extends PruebaApi {

    private static final String YO = "/api/v1/clientes/me";
    private static final String CLAVE = "/api/v1/clientes/me/password";

    @Test
    @DisplayName("CA-1-15 el cliente ve su perfil con correo y DNI")
    void veSuPerfil() throws Exception {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        String token = tokenCliente("ana@correo.com");

        mvc.perform(get(YO).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.data.email").value("ana@correo.com"))
                .andExpect(jsonPath("$.data.dni").value("45678912"))
                .andExpect(jsonPath("$.data.nombres").value("Lucía"))
                .andExpect(jsonPath("$.data.telefono").value("987654321"))
                .andExpect(jsonPath("$.data.creadoEn").isNotEmpty())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("CA-1-15 al editar nombres, apellidos o celular se actualizan; el correo y el DNI no cambian")
    void editaSuPerfil() throws Exception {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        String token = tokenCliente("ana@correo.com");
        Map<String, Object> cuerpo = Map.of("nombres", "Ana María", "apellidos", "Quispe Rojas", "telefono", "912345678",
                "email", "otro@correo.com", "dni", "99999999");

        mvc.perform(put(YO).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(cuerpo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombres").value("Ana María"))
                .andExpect(jsonPath("$.data.apellidos").value("Quispe Rojas"))
                .andExpect(jsonPath("$.data.telefono").value("912345678"))
                .andExpect(jsonPath("$.data.email").value("ana@correo.com"))
                .andExpect(jsonPath("$.data.dni").value("45678912"));

        Map<String, Object> fila = jdbc.queryForMap("SELECT nombres, apellidos, telefono, email, dni FROM cliente WHERE id = ?::uuid",
                cliente.getId().toString());
        assertThat(fila).containsEntry("nombres", "Ana María").containsEntry("telefono", "912345678")
                .containsEntry("email", "ana@correo.com");
        assertThat(fila.get("dni").toString().trim()).isEqualTo("45678912");
    }

    @Test
    @DisplayName("CA-1-15 un celular inválido responde 400 con el campo telefono")
    void editaConCelularInvalido() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String token = tokenCliente("ana@correo.com");

        mvc.perform(put(YO).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("nombres", "Ana", "apellidos", "Quispe", "telefono", "812345678"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("telefono"));
    }

    @Test
    @DisplayName("CA-1-15 sin token responde 401 y con la sesión de un staff responde 403 (RNF-05)")
    void soloElCliente() throws Exception {
        crearStaff("admin@fitstore.pe", RolStaff.ADMIN);
        String staff = tokenStaff("admin@fitstore.pe");

        mvc.perform(get(YO)).andExpect(status().isUnauthorized());
        mvc.perform(get(YO).header("Authorization", bearer(staff)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        mvc.perform(put(YO).header("Authorization", bearer(staff)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombres", "Ana", "apellidos", "Quispe", "telefono", "987654321"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CA-1-16 con la contraseña actual correcta se actualiza, se publica PasswordCambiado y se cierran las otras sesiones")
    void cambiaLaPassword() throws Exception {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        Cookie otroDispositivo = mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana@correo.com", "password", PASSWORD)))
                .andReturn().getResponse().getCookie("fs_refresh");
        String token = tokenCliente("ana@correo.com");

        MvcResult r = mvc.perform(put(CLAVE).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("passwordActual", PASSWORD, "nuevaPassword", "NuevaClave2027"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana@correo.com", "password", "NuevaClave2027")))
                .andExpect(status().isOk());
        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana@correo.com", "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        // las otras sesiones se cierran...
        mvc.perform(post("/api/v1/auth/refresh").cookie(otroDispositivo)).andExpect(status().isUnauthorized());
        // ...pero la sesión que cambió la contraseña sigue con una cookie nueva
        Cookie propia = r.getResponse().getCookie("fs_refresh");
        assertThat(propia).isNotNull();
        mvc.perform(post("/api/v1/auth/refresh").cookie(propia)).andExpect(status().isOk());

        assertThat(eventos("PasswordCambiado")).hasSize(1);
        var evento = json.readTree(eventos("PasswordCambiado").get(0).get("payload").toString());
        assertThat(evento.path("cuentaId").asString()).isEqualTo(cliente.getId().toString());
        assertThat(evento.path("tipoCuenta").asString()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("CA-1-16 una contraseña actual incorrecta responde 400 en el campo passwordActual y no cambia nada")
    void passwordActualIncorrecta() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String token = tokenCliente("ana@correo.com");

        mvc.perform(put(CLAVE).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("passwordActual", "Incorrecta1", "nuevaPassword", "NuevaClave2027"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[0].campo").value("passwordActual"));

        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana@correo.com", "password", PASSWORD))).andExpect(status().isOk());
        assertThat(eventos("PasswordCambiado")).isEmpty();
    }

    @Test
    @DisplayName("CA-1-16 una nueva contraseña que no cumple la política responde 400 en nuevaPassword")
    void nuevaPasswordDebil() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String token = tokenCliente("ana@correo.com");

        mvc.perform(put(CLAVE).header("Authorization", bearer(token)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("passwordActual", PASSWORD, "nuevaPassword", "solo-letras"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nuevaPassword"));
    }
}
