package com.fitstore.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.auth.pruebas.PruebaApi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

/** HU-1-01: registro de clientes (CA-1-01 a CA-1-03). */
class RegistroControllerTest extends PruebaApi {

    private static final String RUTA = "/api/v1/auth/registro";

    private static Map<String, Object> registroValido() {
        Map<String, Object> m = new HashMap<>();
        m.put("email", "Ana.Quispe@Correo.com");
        m.put("password", "Gamarra2026");
        m.put("nombres", "Ana");
        m.put("apellidos", "Quispe Rojas");
        m.put("dni", "45678912");
        m.put("telefono", "987654321");
        m.put("aceptaTerminos", true);
        m.put("aceptaPrivacidad", true);
        m.put("versionTerminos", 3);
        m.put("versionPrivacidad", 2);
        return m;
    }

    private static Map<String, Object> con(String campo, Object valor) {
        Map<String, Object> m = registroValido();
        m.put(campo, valor);
        return m;
    }

    @Test
    @DisplayName("CA-1-01 crea la cuenta, deja la sesión iniciada y guarda dos consentimientos con versión, fecha e IP")
    void registraYIniciaSesion() throws Exception {
        MvcResult r = mvc.perform(postJson(RUTA, registroValido()).header("X-Forwarded-For", "203.0.113.7, 10.0.0.1"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/clientes/me"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cuenta.tipo").value("CLIENTE"))
                .andExpect(jsonPath("$.data.cuenta.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.data.cuenta.email").value("ana.quispe@correo.com"))
                .andExpect(jsonPath("$.data.expiraEn").isNotEmpty())
                .andReturn();

        // sesión iniciada: access token en el cuerpo y refresh en la cookie de A4
        assertThat(cuerpo(r).path("data").path("accessToken").asString()).isNotBlank();
        String cookie = r.getResponse().getHeader("Set-Cookie");
        assertThat(cookie).startsWith("fs_refresh=").contains("HttpOnly", "SameSite=Strict", "Path=/api/v1/auth");

        // la cuenta existe con el correo normalizado y la contraseña con BCrypt
        Map<String, Object> cliente = jdbc.queryForMap("SELECT id, email, password_hash, dni FROM cliente");
        assertThat(cliente.get("email")).isEqualTo("ana.quispe@correo.com");
        assertThat(cliente.get("password_hash").toString()).startsWith("$2").doesNotContain("Gamarra2026");

        // dos consentimientos con la versión que vio el cliente, la fecha y la IP
        List<Map<String, Object>> consentimientos = jdbc.queryForList(
                "SELECT tipo, version_politica, aceptado_en, ip FROM consentimiento WHERE cliente_id = ?::uuid ORDER BY tipo",
                cliente.get("id").toString());
        assertThat(consentimientos).hasSize(2);
        assertThat(consentimientos).extracting(c -> c.get("tipo")).containsExactly("PRIVACIDAD", "TERMINOS");
        assertThat(consentimientos).extracting(c -> c.get("version_politica")).containsExactly("2", "3");
        assertThat(consentimientos).allSatisfy(c -> {
            assertThat(c.get("aceptado_en")).isNotNull();
            assertThat(c.get("ip")).isEqualTo("203.0.113.7");
        });

        // ClienteRegistrado en el outbox, en la misma transacción
        List<Map<String, Object>> eventos = eventos("ClienteRegistrado");
        assertThat(eventos).hasSize(1);
        JsonNode payload = json.readTree(eventos.get(0).get("payload").toString());
        assertThat(payload.path("clienteId").asString()).isEqualTo(cliente.get("id").toString());
        assertThat(payload.path("email").asString()).isEqualTo("ana.quispe@correo.com");
        assertThat(payload.path("nombres").asString()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("CA-1-02 un correo repetido (sin importar mayúsculas) responde 409 CORREO_DUPLICADO")
    void correoDuplicado() throws Exception {
        crearCliente("ana.quispe@correo.com", "11111111");

        mvc.perform(postJson(RUTA, registroValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"))
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado"));
        assertThat(eventos("ClienteRegistrado")).isEmpty();
    }

    @Test
    @DisplayName("CA-1-02 un DNI repetido responde 409 DNI_DUPLICADO")
    void dniDuplicado() throws Exception {
        crearCliente("otra@correo.com", "45678912");

        mvc.perform(postJson(RUTA, registroValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DNI_DUPLICADO"))
                .andExpect(jsonPath("$.mensaje").value("El DNI ya está registrado"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cliente", Integer.class)).isEqualTo(1);
    }

    static java.util.stream.Stream<Arguments> datosInvalidos() {
        return java.util.stream.Stream.of(
                Arguments.of("dni", "1234567", "dni"),
                Arguments.of("dni", "123456789", "dni"),
                Arguments.of("dni", "1234567a", "dni"),
                Arguments.of("telefono", "812345678", "telefono"),
                Arguments.of("telefono", "98765432", "telefono"),
                Arguments.of("telefono", "9876543210", "telefono"),
                Arguments.of("password", "abcdefgh", "password"),
                Arguments.of("password", "12345678", "password"),
                Arguments.of("password", "Ab1", "password"),
                Arguments.of("aceptaTerminos", false, "aceptaTerminos"),
                Arguments.of("aceptaPrivacidad", false, "aceptaPrivacidad"),
                Arguments.of("email", "no-es-un-correo", "email"),
                Arguments.of("nombres", "A", "nombres"));
    }

    @ParameterizedTest(name = "CA-1-03 {0}={1} responde 400 con el campo {2}")
    @MethodSource("datosInvalidos")
    @DisplayName("CA-1-03 el backend responde 400 con los campos inválidos")
    void datosInvalidosResponden400(String campo, Object valor, String esperado) throws Exception {
        MvcResult r = mvc.perform(postJson(RUTA, con(campo, valor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andReturn();

        List<String> campos = cuerpo(r).path("errores").valueStream()
                .map(e -> e.path("campo").asString()).collect(Collectors.toList());
        assertThat(campos).contains(esperado);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM cliente", Integer.class)).isZero();
    }
}
