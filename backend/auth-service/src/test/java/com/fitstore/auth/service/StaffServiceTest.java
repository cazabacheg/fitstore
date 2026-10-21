package com.fitstore.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.pruebas.PruebaApi;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;

/** HU-1-08: administración del personal por un ADMIN (CA-1-25 a CA-1-31). */
class StaffServiceTest extends PruebaApi {

    private static final String RUTA = "/api/v1/staff";

    private Staff admin;
    private String token;

    @BeforeEach
    void adminConSesion() throws Exception {
        admin = crearStaff("admin@fitstore.pe", RolStaff.ADMIN);
        token = tokenStaff("admin@fitstore.pe");
    }

    private MockHttpServletRequestBuilder conCuerpo(MockHttpServletRequestBuilder peticion, String jwt, Object cuerpo) {
        return peticion.header("Authorization", bearer(jwt)).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo));
    }

    private static Map<String, Object> invitacion(String email, String rol) {
        Map<String, Object> m = new HashMap<>();
        m.put("email", email);
        m.put("nombres", "Luis");
        m.put("apellidos", "Rojas");
        m.put("rol", rol);
        return m;
    }

    private JsonNode invitar(String email, String rol) throws Exception {
        MvcResult r = mvc.perform(conCuerpo(post(RUTA), token, invitacion(email, rol))).andExpect(status().isCreated()).andReturn();
        return cuerpo(r).path("data");
    }

    private JsonNode ultimoEvento(String tipo) {
        List<Map<String, Object>> filas = eventos(tipo);
        assertThat(filas).isNotEmpty();
        return json.readTree(filas.get(filas.size() - 1).get("payload").toString());
    }

    private static String tokenDe(String enlace) {
        String token = enlace.substring(enlace.indexOf("token=") + 6);
        return token.contains("&") ? token.substring(0, token.indexOf('&')) : token;
    }

    private int versionDe(Staff staff) {
        return personal.findById(staff.getId()).orElseThrow().getVersion();
    }

    private String estadoDe(Staff staff) {
        return personal.findById(staff.getId()).orElseThrow().estado().name();
    }

    // ---------- CA-1-25 ----------

    @Test
    @DisplayName("CA-1-25 al invitar se crea sin contraseña como invitación pendiente y se publica el evento con motivo INVITACION")
    void invita() throws Exception {
        MvcResult r = mvc.perform(conCuerpo(post(RUTA), token, invitacion("Luis.Rojas@FitStore.pe", "OPERADOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("luis.rojas@fitstore.pe"))
                .andExpect(jsonPath("$.data.rol").value("OPERADOR"))
                .andExpect(jsonPath("$.data.estado").value("INVITACION_PENDIENTE"))
                .andReturn();

        String id = cuerpo(r).path("data").path("id").asString();
        assertThat(r.getResponse().getHeader("Location")).isEqualTo(RUTA + "/" + id);
        Map<String, Object> fila = jdbc.queryForMap("SELECT password_hash, activo, creado_por FROM staff WHERE id = ?::uuid", id);
        assertThat(fila.get("password_hash")).isNull();
        assertThat(fila.get("activo")).isEqualTo(true);
        assertThat(fila.get("creado_por").toString()).isEqualTo(admin.getId().toString());

        JsonNode evento = ultimoEvento("PasswordResetSolicitado");
        assertThat(evento.path("cuentaId").asString()).isEqualTo(id);
        assertThat(evento.path("tipoCuenta").asString()).isEqualTo("STAFF");
        assertThat(evento.path("motivo").asString()).isEqualTo("INVITACION");
        assertThat(evento.path("email").asString()).isEqualTo("luis.rojas@fitstore.pe");
        assertThat(evento.path("enlace").asString()).startsWith("http://admin.prueba/restablecer?token=").endsWith("&invitacion=1");
        Duration vigencia = Duration.between(Instant.now(), Instant.parse(evento.path("expiraEn").asString()));
        assertThat(vigencia).isBetween(Duration.ofHours(23), Duration.ofHours(25));
    }

    @Test
    @DisplayName("CA-1-25 un correo de staff repetido responde 409 y un rol desconocido responde 400")
    void invitacionInvalida() throws Exception {
        mvc.perform(conCuerpo(post(RUTA), token, invitacion("ADMIN@fitstore.pe", "OPERADOR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CORREO_DUPLICADO"));
        mvc.perform(conCuerpo(post(RUTA), token, invitacion("nuevo@fitstore.pe", "SUPERUSUARIO")))
                .andExpect(status().isBadRequest());
        Map<String, Object> sinNombre = invitacion("nuevo@fitstore.pe", "OPERADOR");
        sinNombre.put("nombres", "");
        mvc.perform(conCuerpo(post(RUTA), token, sinNombre))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[0].campo").value("nombres"));
        assertThat(eventos("PasswordResetSolicitado")).isEmpty();
    }

    @Test
    @DisplayName("CA-1-25 el listado pagina y filtra por rol, estado y texto (nombre o correo)")
    void listaYFiltra() throws Exception {
        invitar("pendiente@fitstore.pe", "OPERADOR");
        Staff inactivo = crearStaff("inactivo@fitstore.pe", RolStaff.OPERADOR);
        inactivo.setActivo(false);
        personal.saveAndFlush(inactivo);
        crearStaff("marta@fitstore.pe", RolStaff.OPERADOR);

        mvc.perform(get(RUTA).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElementos").value(4))
                .andExpect(jsonPath("$.data.pagina").value(0))
                .andExpect(jsonPath("$.data.tamano").value(20))
                .andExpect(jsonPath("$.data.totalPaginas").value(1))
                .andExpect(jsonPath("$.data.contenido.length()").value(4));
        mvc.perform(get(RUTA).param("rol", "ADMIN").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.totalElementos").value(1));
        mvc.perform(get(RUTA).param("estado", "INVITACION_PENDIENTE").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.totalElementos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].email").value("pendiente@fitstore.pe"));
        mvc.perform(get(RUTA).param("estado", "INACTIVO").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.totalElementos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].email").value("inactivo@fitstore.pe"));
        mvc.perform(get(RUTA).param("estado", "ACTIVO").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.totalElementos").value(2));
        mvc.perform(get(RUTA).param("q", "MARTA").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.totalElementos").value(1))
                .andExpect(jsonPath("$.data.contenido[0].email").value("marta@fitstore.pe"));
        mvc.perform(get(RUTA).param("size", "2").param("page", "1").param("sort", "email,asc").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.contenido.length()").value(2))
                .andExpect(jsonPath("$.data.totalPaginas").value(2))
                .andExpect(jsonPath("$.data.contenido[0].email").value("marta@fitstore.pe"));
        mvc.perform(get(RUTA).param("sort", "passwordHash,asc").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-1-25 se puede ver un miembro del staff por su id y un id inexistente responde 404")
    void veUnMiembro() throws Exception {
        mvc.perform(get(RUTA + "/" + admin.getId()).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@fitstore.pe"));
        mvc.perform(get(RUTA + "/" + java.util.UUID.randomUUID()).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    // ---------- CA-1-26 ----------

    @Test
    @DisplayName("CA-1-26 con el enlace de invitación el invitado crea su contraseña, queda activo y puede ingresar")
    void invitadoCreaSuPassword() throws Exception {
        JsonNode invitado = invitar("luis@fitstore.pe", "OPERADOR");
        mvc.perform(postJson("/api/v1/auth/staff/login", Map.of("email", "luis@fitstore.pe", "password", "Clave2026x")))
                .andExpect(status().isUnauthorized());
        String enlace = ultimoEvento("PasswordResetSolicitado").path("enlace").asString();

        mvc.perform(postJson("/api/v1/auth/password/restablecer", Map.of("token", tokenDe(enlace), "nuevaPassword", "Clave2026x")))
                .andExpect(status().isOk());

        mvc.perform(postJson("/api/v1/auth/staff/login", Map.of("email", "luis@fitstore.pe", "password", "Clave2026x")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuenta.rol").value("OPERADOR"));
        mvc.perform(get(RUTA + "/" + invitado.path("id").asString()).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"));
    }

    // ---------- CA-1-27 ----------

    @Test
    @DisplayName("CA-1-27 se edita el nombre y el rol; el correo no cambia")
    void edita() throws Exception {
        Staff op = crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        Map<String, Object> cambios = new HashMap<>();
        cambios.put("nombres", "Marco Antonio");
        cambios.put("apellidos", "Rojas Díaz");
        cambios.put("rol", "ADMIN");
        cambios.put("version", op.getVersion());
        cambios.put("email", "otro@fitstore.pe");

        mvc.perform(conCuerpo(put(RUTA + "/" + op.getId()), token, cambios))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombres").value("Marco Antonio"))
                .andExpect(jsonPath("$.data.rol").value("ADMIN"))
                .andExpect(jsonPath("$.data.email").value("op@fitstore.pe"));
        assertThat(jdbc.queryForObject("SELECT rol FROM staff WHERE id = ?::uuid", String.class, op.getId().toString())).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("CA-1-27 si otro usuario lo modificó antes (versión distinta) responde 409 CONFLICTO_VERSION")
    void conflictoDeVersion() throws Exception {
        Staff op = crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        Map<String, Object> cambios = Map.of("nombres", "Marco", "apellidos", "Rojas", "rol", "OPERADOR", "version", op.getVersion() + 1);

        mvc.perform(conCuerpo(put(RUTA + "/" + op.getId()), token, cambios))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
    }

    // ---------- CA-1-28 ----------

    @Test
    @DisplayName("CA-1-28 no se puede degradar al último ADMIN activo: 409 ULTIMO_ADMIN y nada cambia")
    void noDegradaAlUltimoAdmin() throws Exception {
        Map<String, Object> cambios = Map.of("nombres", "Admin", "apellidos", "Fit", "rol", "OPERADOR", "version", versionDe(admin));

        mvc.perform(conCuerpo(put(RUTA + "/" + admin.getId()), token, cambios))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ULTIMO_ADMIN"));
        assertThat(jdbc.queryForObject("SELECT rol FROM staff WHERE id = ?::uuid", String.class, admin.getId().toString())).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("CA-1-28 un admin no puede desactivarse a sí mismo: 409 AUTO_DESACTIVACION")
    void noSeAutoDesactiva() throws Exception {
        crearStaff("otro-admin@fitstore.pe", RolStaff.ADMIN);

        mvc.perform(patch(RUTA + "/" + admin.getId() + "/desactivar").header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("AUTO_DESACTIVACION"));
        assertThat(estadoDe(admin)).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("CA-1-28 no se puede desactivar ni degradar al último ADMIN activo aunque lo pida otro (token aún vigente)")
    void noDesactivaAlUltimoAdmin() throws Exception {
        Staff otro = crearStaff("otro-admin@fitstore.pe", RolStaff.ADMIN);
        // el solicitante pierde el acceso con un token todavía vigente: queda un único ADMIN activo
        Staff solicitante = personal.findById(admin.getId()).orElseThrow();
        solicitante.setActivo(false);
        personal.saveAndFlush(solicitante);

        mvc.perform(patch(RUTA + "/" + otro.getId() + "/desactivar").header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ULTIMO_ADMIN"));
        Map<String, Object> cambios = Map.of("nombres", "Otro", "apellidos", "Admin", "rol", "OPERADOR", "version", otro.getVersion());
        mvc.perform(conCuerpo(put(RUTA + "/" + otro.getId()), token, cambios))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ULTIMO_ADMIN"));
        assertThat(estadoDe(otro)).isEqualTo("ACTIVO");
    }

    @Test
    @DisplayName("CA-1-28 con otro ADMIN activo sí se puede degradar a un admin")
    void degradaConOtroAdminActivo() throws Exception {
        Staff otro = crearStaff("otro-admin@fitstore.pe", RolStaff.ADMIN);
        Map<String, Object> cambios = Map.of("nombres", "Otro", "apellidos", "Admin", "rol", "OPERADOR", "version", otro.getVersion());

        mvc.perform(conCuerpo(put(RUTA + "/" + otro.getId()), token, cambios))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rol").value("OPERADOR"));
    }

    // ---------- CA-1-29 ----------

    @Test
    @DisplayName("CA-1-29 al desactivar, el miembro no ingresa ni refresca su sesión; al reactivar vuelve a ingresar")
    void desactivaYReactiva() throws Exception {
        crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        Staff op = personal.findByEmailIgnoreCase("op@fitstore.pe").orElseThrow();
        Cookie sesion = mvc.perform(postJson("/api/v1/auth/staff/login", Map.of("email", "op@fitstore.pe", "password", PASSWORD)))
                .andReturn().getResponse().getCookie("fs_refresh");

        mvc.perform(patch(RUTA + "/" + op.getId() + "/desactivar").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("INACTIVO"));

        mvc.perform(postJson("/api/v1/auth/staff/login", Map.of("email", "op@fitstore.pe", "password", PASSWORD)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(sesion)).andExpect(status().isUnauthorized());

        mvc.perform(patch(RUTA + "/" + op.getId() + "/activar").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"));
        mvc.perform(postJson("/api/v1/auth/staff/login", Map.of("email", "op@fitstore.pe", "password", PASSWORD)))
                .andExpect(status().isOk());
        // la sesión anterior a la desactivación sigue invalidada
        mvc.perform(post("/api/v1/auth/refresh").cookie(sesion)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CA-1-29 desactivar o activar un id inexistente responde 404")
    void desactivarInexistente() throws Exception {
        String inexistente = java.util.UUID.randomUUID().toString();
        mvc.perform(patch(RUTA + "/" + inexistente + "/desactivar").header("Authorization", bearer(token))).andExpect(status().isNotFound());
        mvc.perform(patch(RUTA + "/" + inexistente + "/activar").header("Authorization", bearer(token))).andExpect(status().isNotFound());
    }

    // ---------- CA-1-30 ----------

    @Test
    @DisplayName("CA-1-30 al reenviar una invitación pendiente el enlace anterior deja de servir y se publica uno nuevo")
    void reenvia() throws Exception {
        JsonNode invitado = invitar("luis@fitstore.pe", "OPERADOR");
        String viejo = tokenDe(ultimoEvento("PasswordResetSolicitado").path("enlace").asString());

        mvc.perform(post(RUTA + "/" + invitado.path("id").asString() + "/reenviar-invitacion").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        assertThat(eventos("PasswordResetSolicitado")).hasSize(2);
        JsonNode nuevo = ultimoEvento("PasswordResetSolicitado");
        assertThat(nuevo.path("motivo").asString()).isEqualTo("INVITACION");
        assertThat(nuevo.path("enlace").asString()).endsWith("&invitacion=1");
        assertThat(tokenDe(nuevo.path("enlace").asString())).isNotEqualTo(viejo);
        mvc.perform(postJson("/api/v1/auth/password/restablecer", Map.of("token", viejo, "nuevaPassword", "Clave2026x")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        mvc.perform(postJson("/api/v1/auth/password/restablecer",
                Map.of("token", tokenDe(nuevo.path("enlace").asString()), "nuevaPassword", "Clave2026x"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-1-30 reenviar a quien ya activó su cuenta responde 409 y a un id inexistente 404")
    void reenvioInvalido() throws Exception {
        Staff activo = crearStaff("activo@fitstore.pe", RolStaff.OPERADOR);

        mvc.perform(post(RUTA + "/" + activo.getId() + "/reenviar-invitacion").header("Authorization", bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("INVITACION_NO_PENDIENTE"));
        mvc.perform(post(RUTA + "/" + java.util.UUID.randomUUID() + "/reenviar-invitacion").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        assertThat(eventos("PasswordResetSolicitado")).isEmpty();
    }

    // ---------- CA-1-31 ----------

    private static Stream<MockHttpServletRequestBuilder> endpointsDeAdministracion() {
        String id = "5e738102-a490-469f-b0f6-5f1fc2dbf70c";
        return Stream.of(
                get(RUTA), post(RUTA), get(RUTA + "/" + id), put(RUTA + "/" + id),
                patch(RUTA + "/" + id + "/desactivar"), patch(RUTA + "/" + id + "/activar"),
                post(RUTA + "/" + id + "/reenviar-invitacion"));
    }

    @Test
    @DisplayName("CA-1-31 un OPERADOR recibe 403 en todos los endpoints /staff de administración")
    void operadorNoAdministra() throws Exception {
        crearStaff("op@fitstore.pe", RolStaff.OPERADOR);
        String operador = tokenStaff("op@fitstore.pe");

        for (MockHttpServletRequestBuilder peticion : (Iterable<MockHttpServletRequestBuilder>) endpointsDeAdministracion()::iterator) {
            mvc.perform(peticion.header("Authorization", bearer(operador)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        }
    }

    @Test
    @DisplayName("CA-1-31 un cliente recibe 403 y sin token 401 en los endpoints /staff de administración")
    void clienteYAnonimoNoAdministran() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String cliente = tokenCliente("ana@correo.com");

        for (MockHttpServletRequestBuilder peticion : (Iterable<MockHttpServletRequestBuilder>) endpointsDeAdministracion()::iterator) {
            mvc.perform(peticion.header("Authorization", bearer(cliente)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
    }
}
