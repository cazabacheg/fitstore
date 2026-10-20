package com.fitstore.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.auth.pruebas.PostgresAislada;
import com.fitstore.auth.pruebas.RedisPrueba;
import com.fitstore.auth.security.CuentasDemo;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

/** Datos de demostración de dev y demo (SPEC-1 sección 6): migración repetible {@code db/dev/R__datos_demo.sql}. */
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/dev")
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = {PostgresAislada.class, RedisPrueba.class})
class DatosDemoDevTest {

    static final String PASSWORD_DEV = "FitStoreDemo2026";

    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource datos;
    @Autowired MockMvc mvc;

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }

    private List<String> ubigeos(UUID cliente) {
        return jdbc.queryForList("SELECT ubigeo FROM direccion WHERE cliente_id = ? ORDER BY id", String.class, cliente).stream()
                .map(String::trim).toList();
    }

    @Test
    @DisplayName("Demo: 4 clientes con los UUID de ids-demo.md, con consentimientos y direcciones en Lima, Callao y Arequipa")
    void clientesDeDemostracion() {
        assertThat(contar("cliente")).isEqualTo(4);
        for (UUID id : CuentasDemo.CLIENTES) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM cliente WHERE id = ?", Integer.class, id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM consentimiento WHERE cliente_id = ?", Integer.class, id)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM direccion WHERE cliente_id = ? AND predeterminada",
                    Integer.class, id)).isEqualTo(1);
        }
        List<UUID> c = CuentasDemo.CLIENTES;
        assertThat(ubigeos(c.get(0))).hasSize(1).allMatch(u -> u.startsWith("1501"));
        assertThat(ubigeos(c.get(1))).hasSize(2).anyMatch(u -> u.startsWith("1501")).anyMatch(u -> u.startsWith("0701"));
        assertThat(ubigeos(c.get(2))).hasSize(1).allMatch(u -> u.startsWith("0701"));
        assertThat(ubigeos(c.get(3))).hasSize(1).allMatch(u -> u.startsWith("0401"));
    }

    @Test
    @DisplayName("Demo: un OPERADOR con el UUID fijo y ningún ADMIN (el ADMIN lo crea AdminInicial)")
    void operadorDeDemostracion() {
        assertThat(contar("staff")).isEqualTo(1);
        Map<String, Object> staff = jdbc.queryForMap("SELECT rol, activo FROM staff WHERE id = ?", CuentasDemo.STAFF.get(0));
        assertThat(staff).containsEntry("rol", "OPERADOR").containsEntry("activo", true);
    }

    @Test
    @DisplayName("Demo: con la contraseña común de desarrollo ingresan los clientes y el operador")
    void ingresanConLaContrasenaDeDesarrollo() throws Exception {
        List<String> clientes = jdbc.queryForList("SELECT email FROM cliente", String.class);
        for (String email : clientes) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD_DEV + "\"}")).andExpect(status().isOk());
        }
        String operador = jdbc.queryForObject("SELECT email FROM staff", String.class);
        mvc.perform(post("/api/v1/auth/staff/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + operador + "\",\"password\":\"" + PASSWORD_DEV + "\"}")).andExpect(status().isOk());
        assertThat(clientes).allMatch(e -> e.endsWith("@fitstore.demo"));
    }

    @Test
    @DisplayName("Demo: la migración es repetible, correrla de nuevo no duplica nada")
    void esIdempotente() throws Exception {
        int clientes = contar("cliente");
        int direcciones = contar("direccion");
        int consentimientos = contar("consentimiento");

        try (var conexion = datos.getConnection()) {
            ScriptUtils.executeSqlScript(conexion, new ClassPathResource("db/dev/R__datos_demo.sql"));
        }

        assertThat(contar("cliente")).isEqualTo(clientes);
        assertThat(contar("direccion")).isEqualTo(direcciones);
        assertThat(contar("consentimiento")).isEqualTo(consentimientos);
        assertThat(contar("staff")).isEqualTo(1);
    }
}
