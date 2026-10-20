package com.fitstore.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.common.event.EventoProcesado;
import com.fitstore.common.event.EventoProcesadoRepository;
import com.fitstore.common.event.OutboxEvento;
import com.fitstore.common.event.OutboxEventoRepository;
import com.fitstore.delivery.pruebas.PruebaRepositorio;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/** Tablas comunes de infraestructura (ADR-009, infraestructura 10.3) y restricciones de la migración V1. */
@PruebaRepositorio
class EsquemaBaseTest {

    @Autowired
    OutboxEventoRepository outbox;

    @Autowired
    EventoProcesadoRepository procesados;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-06 outbox_evento guarda el payload jsonb y lista los pendientes en orden")
    void outboxGuardaYListaPendientes() {
        OutboxEvento primero = OutboxEvento.nuevo("EntregaActualizada", "e-1", "{\"estado\":\"ASIGNADA\"}");
        OutboxEvento segundo = OutboxEvento.nuevo("EntregaActualizada", "e-2", "{\"estado\":\"EN_RUTA\"}");
        outbox.saveAllAndFlush(List.of(primero, segundo));

        List<OutboxEvento> pendientes = outbox.findByPublicadoEnIsNullOrderByCreadoEnAsc(Pageable.ofSize(10));
        assertThat(pendientes).extracting(OutboxEvento::getAgregadoId).contains("e-1", "e-2");

        outbox.marcarPublicado(primero.getId(), Instant.now());
        assertThat(outbox.findByPublicadoEnIsNullOrderByCreadoEnAsc(Pageable.ofSize(10)))
                .extracting(OutboxEvento::getId).doesNotContain(primero.getId());
        assertThat(jdbc.queryForObject("SELECT payload ->> 'estado' FROM outbox_evento WHERE id = ?", String.class,
                segundo.getId())).isEqualTo("EN_RUTA");
    }

    @Test
    @DisplayName("CA-0a-06 outbox_evento tiene el índice parcial de pendientes")
    void outboxTieneIndiceParcial() {
        String definicion = jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE tablename = 'outbox_evento' AND indexname = 'ix_outbox_pendientes'",
                String.class);

        assertThat(definicion).contains("creado_en").containsIgnoringCase("WHERE (publicado_en IS NULL)");
    }

    @Test
    @DisplayName("CA-0a-06 evento_procesado recuerda un evento y rechaza registrarlo dos veces")
    void eventoProcesadoEsUnico() {
        UUID id = UUID.randomUUID();
        procesados.saveAndFlush(new EventoProcesado(id, Instant.now()));

        assertThat(procesados.existsById(id)).isTrue();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO evento_procesado (evento_id, procesado_en) VALUES (?, now())", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 tarifa_envio rechaza una modalidad distinta de DELIVERY_LOCAL y AGENCIA")
    void tarifaRechazaModalidadInvalida() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO tarifa_envio (modalidad, ubigeo, costo, plazo_dias_min, plazo_dias_max, activa, creado_en, actualizado_en)
                VALUES ('RECOJO', '99', 1, 1, 2, true, now(), now())"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
