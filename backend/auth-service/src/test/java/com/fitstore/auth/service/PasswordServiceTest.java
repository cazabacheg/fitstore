package com.fitstore.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.pruebas.PruebaApi;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** HU-1-04: recuperación de contraseña de clientes y staff, y los eventos que publica (CA-1-12 a CA-1-14). */
class PasswordServiceTest extends PruebaApi {

    private static final String OLVIDO = "/api/v1/auth/password/olvido";
    private static final String OLVIDO_STAFF = "/api/v1/auth/staff/password/olvido";
    private static final String RESTABLECER = "/api/v1/auth/password/restablecer";
    private static final String MENSAJE_OLVIDO = "Si el correo está registrado, te enviaremos un enlace";

    private JsonNode ultimoEvento(String tipo) {
        List<Map<String, Object>> filas = eventos(tipo);
        assertThat(filas).isNotEmpty();
        return json.readTree(filas.get(filas.size() - 1).get("payload").toString());
    }

    private static String tokenDe(String enlace) {
        String token = enlace.substring(enlace.indexOf("token=") + 6);
        return token.contains("&") ? token.substring(0, token.indexOf('&')) : token;
    }

    private static String sha256(String texto) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    }

    private String pedirEnlace(String ruta, String email, String tipoEvento) throws Exception {
        mvc.perform(postJson(ruta, Map.of("email", email))).andExpect(status().isOk());
        return ultimoEvento(tipoEvento).path("enlace").asString();
    }

    // ---------- CA-1-12 ----------

    @Test
    @DisplayName("CA-1-12 una cuenta activa recibe el evento con el enlace de la tienda y el token vigente 30 minutos")
    void olvidoDeUnCliente() throws Exception {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");

        mvc.perform(postJson(OLVIDO, Map.of("email", "Ana@Correo.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_OLVIDO));

        assertThat(eventos("PasswordResetSolicitado")).hasSize(1);
        JsonNode evento = ultimoEvento("PasswordResetSolicitado");
        assertThat(evento.path("cuentaId").asString()).isEqualTo(cliente.getId().toString());
        assertThat(evento.path("tipoCuenta").asString()).isEqualTo("CLIENTE");
        assertThat(evento.path("email").asString()).isEqualTo("ana@correo.com");
        assertThat(evento.path("nombres").asString()).isEqualTo("Lucía");
        assertThat(evento.path("motivo").asString()).isEqualTo("RECUPERACION");
        assertThat(evento.path("enlace").asString()).startsWith("http://tienda.prueba/restablecer?token=")
                .doesNotContain("invitacion");
        Duration vigencia = Duration.between(Instant.now(), Instant.parse(evento.path("expiraEn").asString()));
        assertThat(vigencia).isBetween(Duration.ofMinutes(29), Duration.ofMinutes(31));

        // solo se guarda el hash SHA-256 del token, nunca el token
        String token = tokenDe(evento.path("enlace").asString());
        Map<String, Object> fila = jdbc.queryForMap("SELECT token_hash, motivo, usado_en, cliente_id, staff_id FROM token_password");
        assertThat(fila.get("token_hash").toString().trim()).isEqualTo(sha256(token)).isNotEqualTo(token);
        assertThat(fila.get("motivo")).isEqualTo("RECUPERACION");
        assertThat(fila.get("usado_en")).isNull();
        assertThat(fila.get("cliente_id").toString()).isEqualTo(cliente.getId().toString());
        assertThat(fila.get("staff_id")).isNull();
    }

    @Test
    @DisplayName("CA-1-12 el enlace de un staff apunta al backoffice")
    void olvidoDeUnStaff() throws Exception {
        Staff staff = crearStaff("admin@fitstore.pe", RolStaff.ADMIN);

        mvc.perform(postJson(OLVIDO_STAFF, Map.of("email", "admin@fitstore.pe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_OLVIDO));

        JsonNode evento = ultimoEvento("PasswordResetSolicitado");
        assertThat(evento.path("cuentaId").asString()).isEqualTo(staff.getId().toString());
        assertThat(evento.path("tipoCuenta").asString()).isEqualTo("STAFF");
        assertThat(evento.path("enlace").asString()).startsWith("http://admin.prueba/restablecer?token=");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM token_password WHERE staff_id = ?::uuid", Integer.class,
                staff.getId().toString())).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-1-12 un correo inexistente, una cuenta desactivada o de otro tipo responden igual y no publican nada")
    void siempreLaMismaRespuesta() throws Exception {
        Cliente inactivo = crearCliente("inactivo@correo.com", "11111111");
        inactivo.setActivo(false);
        clientes.saveAndFlush(inactivo);
        crearStaff("admin@fitstore.pe", RolStaff.ADMIN);

        for (String correo : List.of("noexiste@correo.com", "inactivo@correo.com")) {
            mvc.perform(postJson(OLVIDO, Map.of("email", correo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mensaje").value(MENSAJE_OLVIDO));
        }
        // un staff pidiendo el enlace de clientes no tiene cuenta de cliente
        mvc.perform(postJson(OLVIDO, Map.of("email", "admin@fitstore.pe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_OLVIDO));

        assertThat(eventos("PasswordResetSolicitado")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM token_password", Integer.class)).isZero();
    }

    @Test
    @DisplayName("CA-1-12 un correo con formato inválido responde 400")
    void olvidoConCorreoInvalido() throws Exception {
        mvc.perform(postJson(OLVIDO, Map.of("email", "no-es-correo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    // ---------- CA-1-13 ----------

    @Test
    @DisplayName("CA-1-13 con un enlace válido se guarda la contraseña, el enlace queda usado y se publica PasswordCambiado")
    void restablecerConEnlaceValido() throws Exception {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        String token = tokenDe(pedirEnlace(OLVIDO, "ana@correo.com", "PasswordResetSolicitado"));

        mvc.perform(postJson(RESTABLECER, Map.of("token", token, "nuevaPassword", "NuevaClave2027")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana@correo.com", "password", "NuevaClave2027")))
                .andExpect(status().isOk());
        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana@correo.com", "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT usado_en IS NOT NULL FROM token_password", Boolean.class)).isTrue();

        JsonNode evento = ultimoEvento("PasswordCambiado");
        assertThat(eventos("PasswordCambiado")).hasSize(1);
        assertThat(evento.path("cuentaId").asString()).isEqualTo(cliente.getId().toString());
        assertThat(evento.path("tipoCuenta").asString()).isEqualTo("CLIENTE");
        assertThat(evento.path("email").asString()).isEqualTo("ana@correo.com");
        assertThat(evento.path("nombres").asString()).isEqualTo("Lucía");
    }

    @Test
    @DisplayName("CA-1-13 al restablecer se cierran las sesiones abiertas (CA-1-11): el refresh anterior responde 401")
    void restablecerCierraLasSesiones() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        Cookie sesionAnterior = mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana@correo.com", "password", PASSWORD)))
                .andReturn().getResponse().getCookie("fs_refresh");
        String token = tokenDe(pedirEnlace(OLVIDO, "ana@correo.com", "PasswordResetSolicitado"));

        mvc.perform(postJson(RESTABLECER, Map.of("token", token, "nuevaPassword", "NuevaClave2027"))).andExpect(status().isOk());

        mvc.perform(post("/api/v1/auth/refresh").cookie(sesionAnterior)).andExpect(status().isUnauthorized());
        Cookie nueva = mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana@correo.com", "password", "NuevaClave2027")))
                .andReturn().getResponse().getCookie("fs_refresh");
        mvc.perform(post("/api/v1/auth/refresh").cookie(nueva)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-1-13 el enlace de un staff cambia la contraseña del staff y no la del cliente con el mismo correo")
    void restablecerDeStaff() throws Exception {
        crearCliente("mismo@correo.com", "45678912");
        crearStaff("mismo@correo.com", RolStaff.OPERADOR);
        String token = tokenDe(pedirEnlace(OLVIDO_STAFF, "mismo@correo.com", "PasswordResetSolicitado"));

        mvc.perform(postJson(RESTABLECER, Map.of("token", token, "nuevaPassword", "NuevaClave2027"))).andExpect(status().isOk());

        mvc.perform(postJson("/api/v1/auth/staff/login", Map.of("email", "mismo@correo.com", "password", "NuevaClave2027")))
                .andExpect(status().isOk());
        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "mismo@correo.com", "password", PASSWORD)))
                .andExpect(status().isOk());
        assertThat(ultimoEvento("PasswordCambiado").path("tipoCuenta").asString()).isEqualTo("STAFF");
    }

    // ---------- CA-1-14 ----------

    @Test
    @DisplayName("CA-1-14 un enlace ya usado responde 400 Enlace inválido o expirado")
    void enlaceUsado() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String token = tokenDe(pedirEnlace(OLVIDO, "ana@correo.com", "PasswordResetSolicitado"));
        mvc.perform(postJson(RESTABLECER, Map.of("token", token, "nuevaPassword", "NuevaClave2027"))).andExpect(status().isOk());

        mvc.perform(postJson(RESTABLECER, Map.of("token", token, "nuevaPassword", "OtraClave2028")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value("Enlace inválido o expirado"));
    }

    @Test
    @DisplayName("CA-1-14 un enlace vencido responde 400 y no cambia la contraseña")
    void enlaceVencido() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String token = tokenDe(pedirEnlace(OLVIDO, "ana@correo.com", "PasswordResetSolicitado"));
        jdbc.update("UPDATE token_password SET expira_en = now() - interval '1 minute'");

        mvc.perform(postJson(RESTABLECER, Map.of("token", token, "nuevaPassword", "NuevaClave2027")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));

        mvc.perform(postJson("/api/v1/auth/login", Map.of("email", "ana@correo.com", "password", PASSWORD))).andExpect(status().isOk());
        assertThat(eventos("PasswordCambiado")).isEmpty();
    }

    @Test
    @DisplayName("CA-1-14 un enlace reemplazado por uno más nuevo deja de servir")
    void enlaceReemplazado() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String viejo = tokenDe(pedirEnlace(OLVIDO, "ana@correo.com", "PasswordResetSolicitado"));
        String nuevo = tokenDe(pedirEnlace(OLVIDO, "ana@correo.com", "PasswordResetSolicitado"));

        assertThat(nuevo).isNotEqualTo(viejo);
        mvc.perform(postJson(RESTABLECER, Map.of("token", viejo, "nuevaPassword", "NuevaClave2027")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        mvc.perform(postJson(RESTABLECER, Map.of("token", nuevo, "nuevaPassword", "NuevaClave2027"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-1-14 un token inexistente o de una cuenta desactivada responde 400 TOKEN_INVALIDO")
    void tokenInexistenteOCuentaInactiva() throws Exception {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");
        String token = tokenDe(pedirEnlace(OLVIDO, "ana@correo.com", "PasswordResetSolicitado"));
        cliente.setActivo(false);
        clientes.saveAndFlush(cliente);

        mvc.perform(postJson(RESTABLECER, Map.of("token", "x".repeat(43), "nuevaPassword", "NuevaClave2027")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        mvc.perform(postJson(RESTABLECER, Map.of("token", token, "nuevaPassword", "NuevaClave2027")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
    }

    @Test
    @DisplayName("CA-1-13 una contraseña que no cumple la política responde 400 en nuevaPassword y no gasta el enlace")
    void passwordDebil() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String token = tokenDe(pedirEnlace(OLVIDO, "ana@correo.com", "PasswordResetSolicitado"));

        mvc.perform(postJson(RESTABLECER, Map.of("token", token, "nuevaPassword", "sinnumeros")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nuevaPassword"));

        mvc.perform(postJson(RESTABLECER, Map.of("token", token, "nuevaPassword", "NuevaClave2027"))).andExpect(status().isOk());
    }
}
