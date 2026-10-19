package com.fitstore.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.auth.pruebas.PruebaRepositorio;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** V1 crea todo el esquema de la sección 3.1 del micro, incluidas las tablas de eventos que espera fitstore-common. */
@PruebaRepositorio
class EsquemaInicialTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-05 V1 crea las 7 tablas del esquema de auth, también outbox_evento y evento_procesado")
    void creaLasTablas() {
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tablas).contains("cliente", "staff", "direccion", "consentimiento", "token_password",
                "outbox_evento", "evento_procesado");
    }

    @Test
    @DisplayName("CA-0a-05 V1 crea los índices únicos sobre lower(email), el parcial de dirección y el de outbox")
    void creaLosIndices() {
        List<String> indices = jdbc.queryForList(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public'", String.class);

        assertThat(indices).anyMatch(d -> d.contains("UNIQUE") && d.contains("ON public.cliente") && d.contains("lower("));
        assertThat(indices).anyMatch(d -> d.contains("UNIQUE") && d.contains("ON public.staff") && d.contains("lower("));
        assertThat(indices).anyMatch(d -> d.contains("UNIQUE") && d.contains("ON public.direccion")
                && d.contains("WHERE predeterminada"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.outbox_evento") && d.contains("publicado_en IS NULL"));
    }
}
