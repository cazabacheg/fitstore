package com.fitstore.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.order.pruebas.PruebaRepositorio;
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
    @DisplayName("CA-0a-05 V1 crea las 4 tablas de order, la secuencia del código y las tablas de eventos")
    void creaLasTablas() {
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);
        List<String> secuencias = jdbc.queryForList(
                "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public'", String.class);

        assertThat(tablas).contains("pedido", "pedido_item", "pedido_historial", "pedido_reembolso",
                "outbox_evento", "evento_procesado");
        assertThat(secuencias).contains("pedido_codigo_seq");
    }

    @Test
    @DisplayName("CA-0a-05 V1 crea los índices del micro: parcial de expiración, por cliente, por estado y de outbox")
    void creaLosIndices() {
        List<String> indices = jdbc.queryForList(
                "SELECT indexdef FROM pg_indexes WHERE schemaname = 'public'", String.class);

        assertThat(indices).anyMatch(d -> d.contains("UNIQUE") && d.contains("ON public.pedido ") && d.contains("(codigo)"));
        assertThat(indices).anyMatch(d -> d.contains("UNIQUE") && d.contains("ON public.pedido ")
                && d.contains("(cliente_id, idempotency_key)"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.pedido ") && d.contains("(cliente_id, creado_en DESC)"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.pedido ") && d.contains("(estado, creado_en)"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.pedido ") && d.contains("(expira_en)")
                && d.contains("PENDIENTE_PAGO"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.pedido_item") && d.contains("(producto_id)"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.pedido_historial") && d.contains("(pedido_id, creado_en)"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.pedido_historial") && d.contains("(estado_nuevo, creado_en)"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.pedido_reembolso") && d.contains("(pedido_id)"));
        assertThat(indices).anyMatch(d -> d.contains("ON public.outbox_evento") && d.contains("publicado_en IS NULL"));
    }
}
