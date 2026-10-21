package com.fitstore.delivery.pruebas;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base de las pruebas de API: contexto completo con PostgreSQL real (Testcontainers) y el reloj fijo en el lunes 19 de
 * octubre de 2026 (hora de Lima). Cada prueba parte de las tablas de entregas y repartidores vacías; la semilla de
 * tarifas y agencias (V2) se conserva, salvo que la prueba la cambie y la restaure.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
@Import(PruebaApi.RelojFijo.class)
public abstract class PruebaApi {

    /** Lunes 19 de octubre de 2026, 10:00 en Lima. */
    public static final Instant AHORA = Instant.parse("2026-10-19T15:00:00Z");

    @Autowired protected MockMvc mvc;
    @Autowired protected JsonMapper json;
    @Autowired protected JdbcTemplate jdbc;
    @PersistenceContext protected EntityManager em;

    @BeforeEach
    @AfterEach
    void limpiarEntregas() {
        jdbc.update("DELETE FROM entrega_evento");
        jdbc.update("DELETE FROM entrega");
        jdbc.update("DELETE FROM repartidor");
        jdbc.update("DELETE FROM outbox_evento");
        jdbc.update("DELETE FROM evento_procesado");
    }

    protected String cuerpo(Object datos) {
        return json.writeValueAsString(datos);
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
