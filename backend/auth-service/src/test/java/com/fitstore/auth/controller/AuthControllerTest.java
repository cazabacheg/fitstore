package com.fitstore.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.pruebas.PruebaApi;
import com.fitstore.auth.security.TokenService;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

/** HU-1-02, HU-1-03 y HU-1-07: login de cliente y de staff, bloqueo por intentos, refresh y logout. */
class AuthControllerTest extends PruebaApi {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String LOGIN_STAFF = "/api/v1/auth/staff/login";
    private static final String REFRESH = "/api/v1/auth/refresh";
    private static final String LOGOUT = "/api/v1/auth/logout";

    private Map<String, String> credenciales(String email, String password) {
        return Map.of("email", email, "password", password);
    }

    private JsonNode payloadDe(String jwt) {
        String cuerpo = jwt.split("\\.")[1];
        return json.readTree(new String(Base64.getUrlDecoder().decode(cuerpo), StandardCharsets.UTF_8));
    }

    private Cookie cookieDe(MvcResult resultado) {
        return resultado.getResponse().getCookie("fs_refresh");
    }

    // ---------- HU-1-02: login de clientes ----------

    @Test
    @DisplayName("CA-1-04 un cliente activo ingresa: access token en el cuerpo, refresh en la cookie HttpOnly")
    void clienteIngresa() throws Exception {
        Cliente cliente = crearCliente("ana@correo.com", "45678912");

        MvcResult r = mvc.perform(postJson(LOGIN, credenciales("Ana@Correo.com", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuenta.tipo").value("CLIENTE"))
                .andExpect(jsonPath("$.data.cuenta.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.data.cuenta.email").value("ana@correo.com"))
                .andReturn();

        JsonNode data = cuerpo(r).path("data");
        JsonNode claims = payloadDe(data.path("accessToken").asString());
        assertThat(claims.path("sub").asString()).isEqualTo(cliente.getId().toString());
        assertThat(claims.path("tipo").asString()).isEqualTo("CLIENTE");
        assertThat(claims.path("rol").asString()).isEqualTo("CLIENTE");
        assertThat(claims.path("email").asString()).isEqualTo("ana@correo.com");
        assertThat(claims.path("iss").asString()).isEqualTo("http://localhost:8080");
        assertThat(claims.path("jti").asString()).isNotBlank();
        // vida de 15 minutos y sin datos sensibles
        assertThat(claims.path("exp").asLong() - claims.path("iat").asLong()).isEqualTo(900);
        assertThat(claims.has("dni")).isFalse();
        assertThat(Instant.parse(data.path("expiraEn").asString())).isAfter(Instant.now());

        Cookie cookie = cookieDe(r);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(r.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");
        JsonNode refresh = payloadDe(cookie.getValue());
        assertThat(refresh.path("typ").asString()).isEqualTo("refresh");
        assertThat(refresh.path("sub").asString()).isEqualTo(cliente.getId().toString());

        assertThat(jdbc.queryForObject("SELECT ultimo_login_en IS NOT NULL FROM cliente", Boolean.class)).isTrue();
    }

    @Test
    @DisplayName("CA-1-05 correo inexistente, contraseña errada y cuenta desactivada responden lo mismo")
    void mismoMensajeEnLosTresCasos() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        Cliente inactivo = crearCliente("inactivo@correo.com", "11111111");
        inactivo.setActivo(false);
        clientes.saveAndFlush(inactivo);

        for (Map<String, String> intento : new Map[] {
            credenciales("noexiste@correo.com", PASSWORD),
            credenciales("ana@correo.com", "OtraClave99"),
            credenciales("inactivo@correo.com", PASSWORD)}) {
            mvc.perform(postJson(LOGIN, intento))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"))
                    .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
        }
    }

    @Test
    @DisplayName("CA-1-06 tras 5 intentos fallidos, el sexto responde 423 aunque la contraseña sea correcta")
    void bloqueoTrasCincoFallos() throws Exception {
        crearCliente("ana@correo.com", "45678912");

        for (int i = 0; i < 5; i++) {
            mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", "Incorrecta1")))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", PASSWORD)))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"))
                .andExpect(jsonPath("$.mensaje").value("Cuenta bloqueada temporalmente"));

        Long segundos = redis.getExpire("auth:login:bloqueo:cliente:ana@correo.com");
        assertThat(segundos).isBetween(1L, 900L);
    }

    @Test
    @DisplayName("CA-1-07 las credenciales de un miembro del staff no sirven en la tienda")
    void staffNoIngresaEnLaTienda() throws Exception {
        crearStaff("marco@fitstore.pe", RolStaff.OPERADOR);

        mvc.perform(postJson(LOGIN, credenciales("marco@fitstore.pe", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    // ---------- HU-1-07: login del backoffice ----------

    @Test
    @DisplayName("CA-1-22 un staff activo ingresa al backoffice con su rol en el token")
    void staffIngresa() throws Exception {
        Staff admin = crearStaff("admin@fitstore.pe", RolStaff.ADMIN);

        MvcResult r = mvc.perform(postJson(LOGIN_STAFF, credenciales("admin@fitstore.pe", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuenta.tipo").value("STAFF"))
                .andExpect(jsonPath("$.data.cuenta.rol").value("ADMIN"))
                .andReturn();

        JsonNode claims = payloadDe(cuerpo(r).path("data").path("accessToken").asString());
        assertThat(claims.path("sub").asString()).isEqualTo(admin.getId().toString());
        assertThat(claims.path("tipo").asString()).isEqualTo("STAFF");
        assertThat(claims.path("rol").asString()).isEqualTo("ADMIN");
        assertThat(cookieDe(r)).isNotNull();
    }

    @Test
    @DisplayName("CA-1-23 un cliente, o un staff con invitación pendiente, no ingresa al backoffice")
    void clienteOInvitadoNoIngresan() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        crearStaffPendiente("nuevo@fitstore.pe", RolStaff.OPERADOR);

        mvc.perform(postJson(LOGIN_STAFF, credenciales("ana@correo.com", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
        mvc.perform(postJson(LOGIN_STAFF, credenciales("nuevo@fitstore.pe", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    @DisplayName("CA-1-24 el bloqueo es independiente: cliente y staff con el mismo correo no se bloquean entre sí")
    void bloqueoIndependiente() throws Exception {
        crearCliente("mismo@correo.com", "45678912");
        crearStaff("mismo@correo.com", RolStaff.OPERADOR);

        for (int i = 0; i < 5; i++) {
            mvc.perform(postJson(LOGIN, credenciales("mismo@correo.com", "Incorrecta1"))).andExpect(status().isUnauthorized());
        }
        mvc.perform(postJson(LOGIN, credenciales("mismo@correo.com", PASSWORD))).andExpect(status().isLocked());
        mvc.perform(postJson(LOGIN_STAFF, credenciales("mismo@correo.com", PASSWORD))).andExpect(status().isOk());

        for (int i = 0; i < 5; i++) {
            mvc.perform(postJson(LOGIN_STAFF, credenciales("mismo@correo.com", "Incorrecta1"))).andExpect(status().isUnauthorized());
        }
        mvc.perform(postJson(LOGIN_STAFF, credenciales("mismo@correo.com", PASSWORD))).andExpect(status().isLocked());
    }

    @Test
    @DisplayName("CA-1-24 un ingreso correcto borra el contador de fallos")
    void ingresoCorrectoBorraElContador() throws Exception {
        crearCliente("ana@correo.com", "45678912");

        for (int i = 0; i < 4; i++) {
            mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", "Incorrecta1"))).andExpect(status().isUnauthorized());
        }
        mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", PASSWORD))).andExpect(status().isOk());
        // el contador volvió a cero: otros 4 fallos no bloquean
        for (int i = 0; i < 4; i++) {
            mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", "Incorrecta1"))).andExpect(status().isUnauthorized());
        }
        mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", PASSWORD))).andExpect(status().isOk());
    }

    // ---------- HU-1-03: sesión ----------

    @Test
    @DisplayName("CA-1-08 al recargar, la cookie de refresh devuelve un access token nuevo y rota el refresh")
    void refrescaConLaCookie() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        MvcResult login = mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", PASSWORD))).andReturn();
        Cookie original = cookieDe(login);

        MvcResult r = mvc.perform(post(REFRESH).cookie(original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuenta.email").value("ana@correo.com"))
                .andReturn();

        String access = cuerpo(r).path("data").path("accessToken").asString();
        assertThat(payloadDe(access).path("tipo").asString()).isEqualTo("CLIENTE");
        Cookie rotada = cookieDe(r);
        assertThat(rotada).isNotNull();
        assertThat(rotada.getValue()).isNotEqualTo(original.getValue());
    }

    @Test
    @DisplayName("CA-1-08 un refresh de staff vuelve a buscar la cuenta en la tabla de staff")
    void refrescaUnStaff() throws Exception {
        crearStaff("admin@fitstore.pe", RolStaff.ADMIN);
        MvcResult login = mvc.perform(postJson(LOGIN_STAFF, credenciales("admin@fitstore.pe", PASSWORD))).andReturn();

        mvc.perform(post(REFRESH).cookie(cookieDe(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cuenta.tipo").value("STAFF"))
                .andExpect(jsonPath("$.data.cuenta.rol").value("ADMIN"));
    }

    @Test
    @DisplayName("CA-1-40 dos recargas seguidas desde el mismo navegador mantienen la sesión")
    void recargasSeguidasMismoNavegador() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        Cookie original = cookieDe(mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", PASSWORD))).andReturn());
        String chrome = "Mozilla/5.0 (X11; Linux x86_64) Chrome/141.0";
        mvc.perform(post(REFRESH).cookie(original).header("User-Agent", chrome)).andExpect(status().isOk());

        mvc.perform(post(REFRESH).cookie(original).header("User-Agent", chrome))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(cookie().exists("fs_refresh"));
        mvc.perform(post(REFRESH).cookie(original).header("User-Agent", "Mozilla/5.0 Firefox/140.0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CA-1-09 un refresh ya usado responde 401 y queda registrado como posible robo")
    void refreshReutilizado() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        Cookie original = cookieDe(mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", PASSWORD))).andReturn());
        mvc.perform(post(REFRESH).cookie(original)).andExpect(status().isOk());

        Logger logger = (Logger) LoggerFactory.getLogger(TokenService.class);
        ListAppender<ILoggingEvent> registro = new ListAppender<>();
        registro.start();
        logger.addAppender(registro);
        try {
            mvc.perform(post(REFRESH).cookie(original))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        } finally {
            logger.detachAppender(registro);
        }
        assertThat(registro.list).anySatisfy(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.WARN);
            assertThat(evento.getFormattedMessage()).containsIgnoringCase("posible robo");
        });
    }

    @Test
    @DisplayName("CA-1-10 al cerrar sesión se revoca el refresh, se borra la cookie y ya no se puede refrescar")
    void cierraLaSesion() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        Cookie cookie = cookieDe(mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", PASSWORD))).andReturn());

        MvcResult r = mvc.perform(post(LOGOUT).cookie(cookie)).andExpect(status().isOk()).andReturn();

        Cookie borrada = cookieDe(r);
        assertThat(borrada).isNotNull();
        assertThat(borrada.getMaxAge()).isZero();
        assertThat(borrada.getValue()).isEmpty();
        assertThat(redis.keys("auth:refresh:revocado:*")).hasSize(1);
        mvc.perform(post(REFRESH).cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CA-1-10 cerrar sesión sin cookie o con una cookie inválida responde 200 y borra la cookie")
    void logoutSinSesionValida() throws Exception {
        mvc.perform(post(LOGOUT)).andExpect(status().isOk());
        mvc.perform(post(LOGOUT).cookie(new Cookie("fs_refresh", "basura"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("CA-1-08 sin cookie, con basura o con un access token en lugar del refresh responde 401")
    void refreshInvalido() throws Exception {
        crearCliente("ana@correo.com", "45678912");
        String access = tokenCliente("ana@correo.com");

        mvc.perform(post(REFRESH)).andExpect(status().isUnauthorized());
        mvc.perform(post(REFRESH).cookie(new Cookie("fs_refresh", "basura"))).andExpect(status().isUnauthorized());
        mvc.perform(post(REFRESH).cookie(new Cookie("fs_refresh", access))).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CA-1-04 un access token vencido o ajeno enviado a un endpoint público de sesión no lo bloquea")
    void tokenVencidoNoImpideIngresar() throws Exception {
        crearCliente("ana@correo.com", "45678912");

        mvc.perform(postJson(LOGIN, credenciales("ana@correo.com", PASSWORD)).header("Authorization", "Bearer vencido.o.roto"))
                .andExpect(status().isOk());
    }
}
