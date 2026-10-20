package com.fitstore.settings.pruebas;

import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base de las pruebas de API: contexto completo con PostgreSQL y Redis reales (Testcontainers),
 * el reloj fijo en el lunes 19 de octubre de 2026 (hora de Lima) y el almacén de archivos en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PruebaApi.RelojFijo.class)
public abstract class PruebaApi {

    /** Lunes 19 de octubre de 2026, 10:00 en Lima. */
    public static final Instant AHORA = Instant.parse("2026-10-19T15:00:00Z");

    @Autowired protected MockMvc mvc;
    @Autowired protected JsonMapper json;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected StringRedisTemplate redis;
    @Autowired protected AlmacenEnMemoria almacen;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        PostgresDePrueba.registrar(registro);
        RedisDePrueba.registrar(registro);
    }

    /** Cada prueba parte sin caché ni archivos de otra prueba. */
    @BeforeEach
    void limpiarCachesYArchivos() {
        Set<String> claves = redis.keys("settings:*");
        if (claves != null && !claves.isEmpty()) {
            redis.delete(claves);
        }
        almacen.vaciar();
    }

    protected String cuerpo(Object datos) {
        return json.writeValueAsString(datos);
    }

    protected <T> T leer(MvcResult resultado, String rutaJson) throws Exception {
        return JsonPath.read(resultado.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
                rutaJson);
    }

    protected List<String> eventosDelTipo(String tipo) {
        return jdbc.queryForList("SELECT payload::text FROM outbox_evento WHERE tipo = ? ORDER BY creado_en", String.class,
                tipo);
    }

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(AHORA, ZoneId.of("America/Lima"));
        }
    }
}
