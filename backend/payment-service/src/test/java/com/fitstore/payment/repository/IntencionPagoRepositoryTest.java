package com.fitstore.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.Datos;
import com.fitstore.payment.pruebas.PruebaRepositorio;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class IntencionPagoRepositoryTest {

    @Autowired IntencionPagoRepository repositorio;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-06 inserta una intención desde PedidoCreado y guarda los jsonb del snapshot")
    void inserta() {
        IntencionPago guardada = repositorio.saveAndFlush(Datos.intencion());
        em.clear();

        IntencionPago leida = repositorio.findById(guardada.getId()).orElseThrow();
        assertThat(leida.getEstado()).isEqualTo(EstadoIntencion.PENDIENTE);
        assertThat(leida.getMonto()).isEqualByComparingTo("119.80");
        assertThat(leida.getItems()).hasSize(1);
        assertThat(leida.getItems().get(0)).containsEntry("sku", "POL-001-M-NEG");
        assertThat(leida.getCliente()).containsEntry("dni", "12345678");
        assertThat(leida.getCreadoEn()).isNotNull();
        assertThat(leida.getActualizadoEn()).isNotNull();
        assertThat(leida.getVersion()).isZero();
    }

    @Test
    @DisplayName("CA-0a-06 actualiza el estado y la versión avanza")
    void actualizaEstado() {
        IntencionPago guardada = repositorio.saveAndFlush(Datos.intencion());
        guardada.setEstado(EstadoIntencion.PAGADA);
        repositorio.saveAndFlush(guardada);
        em.clear();

        IntencionPago leida = repositorio.findById(guardada.getId()).orElseThrow();
        assertThat(leida.getEstado()).isEqualTo(EstadoIntencion.PAGADA);
        assertThat(leida.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("CA-0a-06 busca por pedido y por código de pedido")
    void buscaPorPedidoYCodigo() {
        IntencionPago guardada = repositorio.saveAndFlush(Datos.intencion());

        assertThat(repositorio.findByPedidoId(guardada.getPedidoId())).get().extracting(IntencionPago::getId)
                .isEqualTo(guardada.getId());
        assertThat(repositorio.findByCodigoPedido(guardada.getCodigoPedido())).isNotEmpty();
        assertThat(repositorio.findByPedidoId(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 lista con los filtros del backoffice: estado, cliente y fechas")
    void listaConFiltros() {
        IntencionPago pendiente = repositorio.save(Datos.intencion());
        IntencionPago pagada = Datos.intencion();
        pagada.setEstado(EstadoIntencion.PAGADA);
        pagada.setClienteId(pendiente.getClienteId());
        repositorio.saveAndFlush(pagada);

        assertThat(repositorio.findByEstado(EstadoIntencion.PAGADA, PageRequest.of(0, 10)).getContent())
                .extracting(IntencionPago::getId).contains(pagada.getId()).doesNotContain(pendiente.getId());
        assertThat(repositorio.findByClienteIdOrderByCreadoEnDesc(pendiente.getClienteId())).hasSize(2);
        Instant ahora = Instant.now();
        assertThat(repositorio.findByCreadoEnBetween(ahora.minus(1, ChronoUnit.HOURS), ahora.plus(1, ChronoUnit.HOURS)))
                .extracting(IntencionPago::getId).contains(pendiente.getId(), pagada.getId());
        assertThat(repositorio.findByCreadoEnBetween(ahora.minus(3, ChronoUnit.DAYS), ahora.minus(2, ChronoUnit.DAYS)))
                .isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un segundo pago para el mismo pedido (pedido_id único)")
    void rechazaPedidoDuplicado() {
        IntencionPago primera = repositorio.saveAndFlush(Datos.intencion());
        IntencionPago repetida = Datos.intencion();
        repetida.setPedidoId(primera.getPedidoId());

        assertThatThrownBy(() -> repositorio.saveAndFlush(repetida)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un monto que no es mayor que cero (CHECK monto > 0)")
    void rechazaMontoCero() {
        IntencionPago cero = Datos.intencion();
        cero.setMonto(BigDecimal.ZERO);

        assertThatThrownBy(() -> repositorio.saveAndFlush(cero)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un estado fuera de los valores permitidos (CHECK estado)")
    void rechazaEstadoInvalido() {
        IntencionPago guardada = repositorio.saveAndFlush(Datos.intencion());

        assertThatThrownBy(() -> jdbc.update("UPDATE intencion_pago SET estado = 'INVENTADO' WHERE id = ?",
                guardada.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }
}
