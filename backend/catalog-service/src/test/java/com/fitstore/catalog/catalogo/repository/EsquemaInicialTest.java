package com.fitstore.catalog.catalogo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.catalog.pruebas.PruebaRepositorio;
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
    @DisplayName("CA-0a-05 V1 crea las tablas del módulo catálogo, las del inventario y las de eventos")
    void creaLasTablas() {
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tablas).contains("categoria", "talla", "color", "producto", "producto_categoria", "variante", "imagen",
                "guia_tallas", "tramo_descuento", "configuracion_descuento", "stock", "movimiento_stock", "reserva",
                "reserva_item", "configuracion_inventario", "outbox_evento", "evento_procesado");
    }

    @Test
    @DisplayName("CA-0a-05 V1 instala las extensiones unaccent y pg_trgm")
    void instalaExtensiones() {
        List<String> extensiones = jdbc.queryForList("SELECT extname FROM pg_extension", String.class);

        assertThat(extensiones).contains("unaccent", "pg_trgm");
    }

    @Test
    @DisplayName("CA-0a-05 V1 crea los índices del micro: trigram del nombre, parciales de imagen, reserva y outbox")
    void creaLosIndices() {
        List<String> indices = jdbc.queryForList(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public'", String.class);

        assertThat(indices).anyMatch(d -> d.contains("ON public.producto") && d.contains("gin") && d.contains("gin_trgm_ops"));
        assertThat(indices).anyMatch(d -> d.contains("UNIQUE") && d.contains("ON public.imagen") && d.contains("WHERE principal"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.reserva ") && d.contains("expira_en") && d.contains("ACTIVA"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.outbox_evento") && d.contains("publicado_en IS NULL"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.producto ") && d.contains("(precio_desde)"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.producto ") && d.contains("(estado)"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.producto ") && d.contains("(creado_en)"));
    }

    @Test
    @DisplayName("CA-0a-05 V1 usa NULLS NOT DISTINCT en las unicidades con columna opcional (categoría y tramo)")
    void unicidadesConNulos() {
        List<String> indices = jdbc.queryForList(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public'", String.class);

        assertThat(indices).anyMatch(d -> d.contains("UNIQUE") && d.contains("ON public.categoria")
                && d.contains("lower(") && d.contains("NULLS NOT DISTINCT"));
        assertThat(indices).anyMatch(d -> d.contains("UNIQUE") && d.contains("ON public.tramo_descuento")
                && d.contains("NULLS NOT DISTINCT"));
    }

    @Test
    @DisplayName("CA-0a-05 outbox_evento y evento_procesado tienen los tipos que espera fitstore-common")
    void tiposDeEventos() {
        assertThat(tipo("outbox_evento", "id")).isEqualTo("uuid");
        assertThat(tipo("outbox_evento", "tipo")).isEqualTo("character varying");
        assertThat(tipo("outbox_evento", "agregado_id")).isEqualTo("character varying");
        assertThat(tipo("outbox_evento", "payload")).isEqualTo("jsonb");
        assertThat(tipo("outbox_evento", "creado_en")).isEqualTo("timestamp with time zone");
        assertThat(tipo("outbox_evento", "publicado_en")).isEqualTo("timestamp with time zone");
        assertThat(tipo("evento_procesado", "evento_id")).isEqualTo("uuid");
        assertThat(tipo("evento_procesado", "procesado_en")).isEqualTo("timestamp with time zone");
    }

    private String tipo(String tabla, String columna) {
        return jdbc.queryForObject("SELECT data_type FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?", String.class, tabla, columna);
    }
}
