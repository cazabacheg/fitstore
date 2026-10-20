package com.fitstore.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.payment.pruebas.PruebaRepositorio;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** CA-0a-05: Flyway aplica V1 con todo el esquema y Hibernate valida (ddl-auto=validate) que las entidades coinciden. */
@PruebaRepositorio
class EsquemaInicialTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-05 V1 crea las tablas del micro 3.1 y las de eventos de fitstore-common")
    void tablas() {
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tablas).contains("intencion_pago", "transaccion", "reembolso", "boleta", "correlativo_boleta",
                "webhook_recibido", "outbox_evento", "evento_procesado");
    }

    @Test
    @DisplayName("CA-0a-05 Flyway aplicó V1 con éxito")
    void flywayAplicoV1() {
        Integer aplicadas = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success", Integer.class);
        assertThat(aplicadas).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-05 outbox_evento y evento_procesado tienen los tipos que espera fitstore-common")
    void tablasDeEventos() {
        assertThat(tipo("outbox_evento", "id")).isEqualTo("uuid");
        assertThat(tipo("outbox_evento", "tipo")).isEqualTo("character varying");
        assertThat(tipo("outbox_evento", "agregado_id")).isEqualTo("character varying");
        assertThat(tipo("outbox_evento", "payload")).isEqualTo("jsonb");
        assertThat(tipo("outbox_evento", "creado_en")).isEqualTo("timestamp with time zone");
        assertThat(tipo("outbox_evento", "publicado_en")).isEqualTo("timestamp with time zone");
        assertThat(tipo("evento_procesado", "evento_id")).isEqualTo("uuid");
        assertThat(tipo("evento_procesado", "procesado_en")).isEqualTo("timestamp with time zone");
        Integer indicePendientes = jdbc.queryForObject(
                "SELECT count(*) FROM pg_indexes WHERE indexname = 'ix_outbox_pendientes'", Integer.class);
        assertThat(indicePendientes).isEqualTo(1);
    }

    private String tipo(String tabla, String columna) {
        return jdbc.queryForObject("SELECT data_type FROM information_schema.columns "
                + "WHERE table_name = ? AND column_name = ?", String.class, tabla, columna);
    }
}
