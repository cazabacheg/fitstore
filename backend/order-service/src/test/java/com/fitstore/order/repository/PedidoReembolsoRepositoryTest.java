package com.fitstore.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.order.pedidos.model.EstadoReembolso;
import com.fitstore.order.pedidos.model.ItemReembolso;
import com.fitstore.order.pedidos.model.MotivoReembolso;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.PedidoReembolso;
import com.fitstore.order.pedidos.repository.PedidoReembolsoRepository;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pruebas.Datos;
import com.fitstore.order.pruebas.PruebaRepositorio;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** CA-0a-06: capa de datos de {@code PedidoReembolsoRepository} (reembolso postventa, micro 3.1). */
@PruebaRepositorio
class PedidoReembolsoRepositoryTest {

    private static final java.util.concurrent.atomic.AtomicInteger CONTADOR =
            new java.util.concurrent.atomic.AtomicInteger();

    @Autowired PedidoReembolsoRepository repositorio;
    @Autowired PedidoRepository pedidos;
    @Autowired TestEntityManager em;
    @Autowired JdbcTemplate jdbc;

    private Pedido pedido() {
        return pedidos.saveAndFlush(Datos.pedido("FS-" + (100000 + CONTADOR.incrementAndGet()),
                UUID.randomUUID(), UUID.randomUUID().toString()));
    }

    private PedidoReembolso reembolso(Pedido pedido, String monto) {
        PedidoReembolso r = new PedidoReembolso();
        r.setPedidoId(pedido.getId());
        r.setMonto(new BigDecimal(monto));
        r.setMotivo(MotivoReembolso.DEFECTUOSO);
        r.setNota("Costura abierta");
        r.setItems(List.of(new ItemReembolso(10L, "POL-NEG-M", 1)));
        r.setSolicitadoPor(UUID.randomUUID());
        return r;
    }

    @Test
    @DisplayName("CA-0a-06 inserta un reembolso con sus ítems en jsonb y estado SOLICITADO")
    void inserta() {
        Pedido pedido = pedido();
        PedidoReembolso guardado = repositorio.saveAndFlush(reembolso(pedido, "50.00"));
        em.clear();

        PedidoReembolso recargado = repositorio.findById(guardado.getId()).orElseThrow();
        assertThat(recargado.getEstado()).isEqualTo(EstadoReembolso.SOLICITADO);
        assertThat(recargado.getMotivo()).isEqualTo(MotivoReembolso.DEFECTUOSO);
        assertThat(recargado.getItems()).containsExactly(new ItemReembolso(10L, "POL-NEG-M", 1));
        assertThat(recargado.getCreadoEn()).isNotNull();
        assertThat(recargado.getCompletadoEn()).isNull();
    }

    @Test
    @DisplayName("CA-0a-06 actualiza el reembolso a COMPLETADO al recibir PagoReembolsado")
    void completa() {
        PedidoReembolso guardado = repositorio.saveAndFlush(reembolso(pedido(), "50.00"));
        guardado.setEstado(EstadoReembolso.COMPLETADO);
        guardado.setCompletadoEn(Instant.parse("2026-10-20T09:00:00Z"));
        repositorio.saveAndFlush(guardado);
        em.clear();

        PedidoReembolso recargado = repositorio.findById(guardado.getId()).orElseThrow();
        assertThat(recargado.getEstado()).isEqualTo(EstadoReembolso.COMPLETADO);
        assertThat(recargado.getCompletadoEn()).isEqualTo(Instant.parse("2026-10-20T09:00:00Z"));
    }

    @Test
    @DisplayName("CA-0a-06 lista los reembolsos de un pedido en orden de creación")
    void listaPorPedido() {
        Pedido pedido = pedido();
        PedidoReembolso primero = reembolso(pedido, "10.00");
        primero.setCreadoEn(Instant.parse("2026-10-20T09:00:00Z"));
        PedidoReembolso segundo = reembolso(pedido, "20.00");
        segundo.setCreadoEn(Instant.parse("2026-10-21T09:00:00Z"));
        repositorio.saveAndFlush(segundo);
        repositorio.saveAndFlush(primero);
        repositorio.saveAndFlush(reembolso(pedido(), "5.00"));

        List<PedidoReembolso> lista = repositorio.findByPedidoIdOrderByCreadoEnAsc(pedido.getId());

        assertThat(lista).extracting(PedidoReembolso::getMonto)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("10.00"), new BigDecimal("20.00"));
    }

    @Test
    @DisplayName("CA-0a-06 elimina un reembolso solicitado (borrado físico)")
    void elimina() {
        PedidoReembolso guardado = repositorio.saveAndFlush(reembolso(pedido(), "50.00"));

        repositorio.deleteById(guardado.getId());
        repositorio.flush();

        assertThat(repositorio.findById(guardado.getId())).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: rechaza un monto igual a cero")
    void montoCero() {
        PedidoReembolso r = reembolso(pedido(), "0.00");

        assertThatThrownBy(() -> repositorio.saveAndFlush(r)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: rechaza un motivo que no existe")
    void motivoInvalido() {
        PedidoReembolso guardado = repositorio.saveAndFlush(reembolso(pedido(), "50.00"));

        assertThatThrownBy(() -> jdbc.update("UPDATE pedido_reembolso SET motivo = 'CAPRICHO' WHERE id = ?",
                guardado.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: rechaza un estado de reembolso que no existe")
    void estadoInvalido() {
        PedidoReembolso guardado = repositorio.saveAndFlush(reembolso(pedido(), "50.00"));

        assertThatThrownBy(() -> jdbc.update("UPDATE pedido_reembolso SET estado = 'RARO' WHERE id = ?",
                guardado.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un reembolso de un pedido que no existe")
    void pedidoInexistente() {
        Pedido fantasma = Datos.pedido("FS-999999", UUID.randomUUID(), "x");
        PedidoReembolso r = reembolso(fantasma, "50.00");

        assertThatThrownBy(() -> repositorio.saveAndFlush(r)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
