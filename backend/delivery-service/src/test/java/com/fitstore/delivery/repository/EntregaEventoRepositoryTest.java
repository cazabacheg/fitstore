package com.fitstore.delivery.repository;

import static com.fitstore.delivery.pruebas.Datos.entrega;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EntregaEvento;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.pruebas.PruebaRepositorio;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class EntregaEventoRepositoryTest {

    @Autowired
    EntregaEventoRepository repositorio;

    @Autowired
    EntregaRepository entregas;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    private EntregaEvento evento(Entrega entrega, EstadoEntrega estado, String observacion, Instant cuando) {
        return EntregaEvento.builder().entrega(entrega).estado(estado).observacion(observacion)
                .actorId(UUID.randomUUID()).creadoEn(cuando).build();
    }

    @Test
    @DisplayName("CA-0a-06 inserta un evento de seguimiento con su actor y su observación")
    void inserta() {
        Entrega entrega = entregas.saveAndFlush(entrega(Modalidad.DELIVERY_LOCAL));

        EntregaEvento guardado = repositorio.saveAndFlush(
                evento(entrega, EstadoEntrega.FALLIDA, "No había nadie", Instant.parse("2026-10-22T15:00:00Z")));
        em.clear();

        EntregaEvento leido = repositorio.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoEntrega.FALLIDA);
        assertThat(leido.getObservacion()).isEqualTo("No había nadie");
        assertThat(leido.getActorId()).isNotNull();
        assertThat(leido.getEntrega().getId()).isEqualTo(entrega.getId());
    }

    @Test
    @DisplayName("CA-0a-06 lista el seguimiento de una entrega en orden cronológico")
    void listaEnOrdenCronologico() {
        Entrega entrega = entregas.saveAndFlush(entrega(Modalidad.DELIVERY_LOCAL));
        Entrega otra = entregas.saveAndFlush(entrega(Modalidad.DELIVERY_LOCAL));
        Instant t0 = Instant.parse("2026-10-22T10:00:00Z");
        repositorio.save(evento(entrega, EstadoEntrega.EN_RUTA, null, t0.plusSeconds(60)));
        repositorio.save(evento(entrega, EstadoEntrega.PENDIENTE_ASIGNACION, null, t0));
        repositorio.save(evento(otra, EstadoEntrega.ASIGNADA, null, t0));
        repositorio.flush();

        assertThat(repositorio.findByEntregaIdOrderByCreadoEnAsc(entrega.getId()))
                .extracting(EntregaEvento::getEstado)
                .containsExactly(EstadoEntrega.PENDIENTE_ASIGNACION, EstadoEntrega.EN_RUTA);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un evento de una entrega que no existe")
    void rechazaEntregaInexistente() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO entrega_evento (entrega_id, estado, creado_en) VALUES (?, 'ASIGNADA', now())""",
                UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un estado fuera de los nueve valores del contrato")
    void rechazaEstadoInvalido() {
        Entrega entrega = entregas.saveAndFlush(entrega(Modalidad.DELIVERY_LOCAL));

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO entrega_evento (entrega_id, estado, creado_en) VALUES (?, 'PERDIDA', now())""",
                entrega.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }
}
