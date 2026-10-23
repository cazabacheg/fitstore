package com.fitstore.auth.pruebas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.repository.ClienteRepository;
import com.fitstore.auth.repository.StaffRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base de las pruebas de API de auth: el servicio completo con PostgreSQL y Redis reales (Testcontainers), MockMvc y
 * tablas vacías antes de cada prueba. Las cuentas se crean directo en la base y las sesiones entran por los endpoints
 * reales, así que los tokens que usan las pruebas son los que emite auth.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = {PostgresPrueba.class, RedisPrueba.class})
public abstract class PruebaApi {

    /** Contraseña de las cuentas que crean las pruebas. */
    public static final String PASSWORD = "Gamarra2026";

    private static final BCryptPasswordEncoder CODIFICADOR = new BCryptPasswordEncoder(4);

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected StringRedisTemplate redis;
    @Autowired protected JsonMapper json;
    @Autowired protected ClienteRepository clientes;
    @Autowired protected StaffRepository personal;

    /** Limpia antes y después: las pruebas de repositorio que corren luego cuentan filas y no deben ver restos. */
    @BeforeEach
    @AfterEach
    void limpiarEstado() {
        jdbc.execute("TRUNCATE TABLE token_password, consentimiento, direccion, cliente, staff, outbox_evento CASCADE");
        var claves = redis.keys("auth:*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
    }

    protected Cliente crearCliente(String email, String dni) {
        Cliente c = Datos.cliente(email, dni);
        c.setPasswordHash(CODIFICADOR.encode(PASSWORD));
        return clientes.saveAndFlush(c);
    }

    protected Staff crearStaff(String email, RolStaff rol) {
        Staff s = Datos.staff(email, rol);
        s.setPasswordHash(CODIFICADOR.encode(PASSWORD));
        return personal.saveAndFlush(s);
    }

    /** Staff invitado que todavía no establece su contraseña ("invitación pendiente"). */
    protected Staff crearStaffPendiente(String email, RolStaff rol) {
        return personal.saveAndFlush(Datos.staff(email, rol));
    }

    protected MockHttpServletRequestBuilder postJson(String ruta, Object cuerpo) {
        return post(ruta).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
    }

    protected JsonNode cuerpo(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    /** Inicia sesión de cliente por el endpoint real y devuelve el access token. */
    protected String tokenCliente(String email) throws Exception {
        MvcResult r = mvc.perform(postJson("/api/v1/auth/login", Map.of("email", email, "password", PASSWORD))).andReturn();
        return cuerpo(r).path("data").path("accessToken").asString();
    }

    /** Inicia sesión de staff por el endpoint real y devuelve el access token. */
    protected String tokenStaff(String email) throws Exception {
        MvcResult r = mvc.perform(postJson("/api/v1/auth/staff/login", Map.of("email", email, "password", PASSWORD))).andReturn();
        return cuerpo(r).path("data").path("accessToken").asString();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Filas del outbox de un tipo de evento, de la más antigua a la más reciente. */
    protected List<Map<String, Object>> eventos(String tipo) {
        return jdbc.queryForList(
                "SELECT tipo, agregado_id, payload::text AS payload FROM outbox_evento WHERE tipo = ? ORDER BY creado_en", tipo);
    }
}
