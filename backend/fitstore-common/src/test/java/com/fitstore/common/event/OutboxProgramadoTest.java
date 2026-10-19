package com.fitstore.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fitstore.common.pruebas.AplicacionDePrueba;
import com.fitstore.common.pruebas.ContenedoresPrueba;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/** CA-0a-14: el publicador programado vacía el outbox solo, sin que el servicio lo invoque. */
@SpringBootTest(
        classes = {AplicacionDePrueba.class, OutboxProgramadoTest.Rutas.class},
        properties = {
            "spring.sql.init.mode=always",
            "spring.sql.init.schema-locations=classpath:esquema-eventos-prueba.sql",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.rabbitmq.publisher-confirm-type=correlated",
            "fitstore.eventos.servicio=programado",
            "fitstore.eventos.enlaces[0]=programado.cosa.creada",
            "fitstore.outbox.habilitado=true",
            "fitstore.outbox.intervalo-ms=100"
        })
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class OutboxProgramadoTest {

    @Configuration
    static class Rutas {
        @Bean
        CatalogoRutasEventos catalogoRutasEventos() {
            return tipo -> "programado.cosa.creada";
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        ContenedoresPrueba.registrarPostgres(registro);
        ContenedoresPrueba.registrarRabbit(registro);
    }

    @Autowired OutboxRegistrador registrador;
    @Autowired TransactionTemplate transaccion;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-14 el publicador programado marca publicado_en sin intervención del servicio")
    void publicaSolo() {
        UUID id = transaccion.execute(e -> registrador.registrar("CosaCreada", "p-1", Map.of("k", "v")));

        await().atMost(Duration.ofSeconds(20)).until(() -> Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT publicado_en IS NOT NULL FROM outbox_evento WHERE id = ?", Boolean.class, id)));

        assertThat(id).isNotNull();
    }
}
