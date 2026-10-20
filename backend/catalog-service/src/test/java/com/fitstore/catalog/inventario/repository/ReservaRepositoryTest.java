package com.fitstore.catalog.inventario.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.catalog.inventario.model.ConfiguracionInventario;
import com.fitstore.catalog.inventario.model.EstadoReserva;
import com.fitstore.catalog.inventario.model.Reserva;
import com.fitstore.catalog.inventario.model.ReservaItem;
import com.fitstore.catalog.pruebas.Datos;
import com.fitstore.catalog.pruebas.PruebaRepositorio;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Reservas y configuración de inventario (apoyo de la reserva atómica de {@code StockRepository}). */
@PruebaRepositorio
class ReservaRepositoryTest {

    @Autowired ReservaRepository reservas;
    @Autowired ReservaItemRepository items;
    @Autowired ConfiguracionInventarioRepository configuracion;
    @Autowired ProductoRepository productos;
    @Autowired VarianteRepository variantes;
    @Autowired TallaRepository tallas;
    @Autowired ColorRepository colores;
    @Autowired JdbcTemplate jdbc;

    Variante variante;

    @BeforeEach
    void variante() {
        Producto p = productos.saveAndFlush(Datos.producto("RES-01", "Legging Reserva", "legging-reserva", "59.90"));
        variante = variantes.saveAndFlush(Datos.variante(p.getId(), tallas.findByCodigo("M").orElseThrow().getId(),
                colores.findByNombreIgnoreCase("negro").orElseThrow().getId(), "RES-01-NEGRO-M"));
    }

    private Reserva reserva(UUID pedidoId, Instant expiraEn) {
        Reserva r = new Reserva();
        r.setPedidoId(pedidoId);
        r.setEstado(EstadoReserva.ACTIVA);
        r.setExpiraEn(expiraEn);
        return r;
    }

    private ReservaItem item(UUID pedidoId, Long varianteId, int cantidad) {
        ReservaItem i = new ReservaItem();
        i.setPedidoId(pedidoId);
        i.setVarianteId(varianteId);
        i.setCantidad(cantidad);
        return i;
    }

    @Test
    @DisplayName("CA-0a-06 ReservaRepository inserta una reserva ACTIVA con sus ítems")
    void inserta() {
        UUID pedido = UUID.randomUUID();
        reservas.saveAndFlush(reserva(pedido, Instant.now().plusSeconds(2100)));
        items.saveAndFlush(item(pedido, variante.getId(), 2));

        Reserva leida = reservas.findById(pedido).orElseThrow();
        assertThat(leida.getEstado()).isEqualTo(EstadoReserva.ACTIVA);
        assertThat(leida.getCreadoEn()).isNotNull();
        assertThat(items.findByPedidoId(pedido)).extracting(ReservaItem::getCantidad).containsExactly(2);
    }

    @Test
    @DisplayName("CA-0a-06 ReservaRepository actualiza el estado de la reserva")
    void actualizaEstado() {
        UUID pedido = UUID.randomUUID();
        Reserva r = reservas.saveAndFlush(reserva(pedido, Instant.now().plusSeconds(60)));
        r.setEstado(EstadoReserva.CONFIRMADA);
        reservas.saveAndFlush(r);

        assertThat(reservas.findById(pedido).orElseThrow().getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(jdbc.queryForObject("SELECT estado FROM reserva WHERE pedido_id = ?", String.class, pedido))
                .isEqualTo("CONFIRMADA");
    }

    @Test
    @DisplayName("CA-0a-06 ReservaRepository lista las reservas ACTIVAS vencidas para el liberador de respaldo")
    void listaVencidas() {
        Instant ahora = Instant.now();
        UUID vencida = UUID.randomUUID();
        UUID vigente = UUID.randomUUID();
        UUID confirmadaVencida = UUID.randomUUID();
        reservas.saveAndFlush(reserva(vencida, ahora.minusSeconds(60)));
        reservas.saveAndFlush(reserva(vigente, ahora.plusSeconds(600)));
        Reserva c = reserva(confirmadaVencida, ahora.minusSeconds(60));
        c.setEstado(EstadoReserva.CONFIRMADA);
        reservas.saveAndFlush(c);

        assertThat(reservas.findByEstadoAndExpiraEnBefore(EstadoReserva.ACTIVA, ahora))
                .extracting(Reserva::getPedidoId).containsExactly(vencida);
    }

    @Test
    @DisplayName("CA-0a-06 ReservaRepository rechaza una segunda reserva del mismo pedido (pedidoId único)")
    void unaReservaPorPedido() {
        UUID pedido = UUID.randomUUID();
        reservas.saveAndFlush(reserva(pedido, Instant.now().plusSeconds(60)));
        assertThat(reservas.existsById(pedido)).isTrue();

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO reserva (pedido_id, estado, expira_en, creado_en, actualizado_en) VALUES (?, 'ACTIVA', now(), now(), now())",
                pedido)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ReservaRepository rechaza un ítem con cantidad cero (CHECK)")
    void cantidadCero() {
        UUID pedido = UUID.randomUUID();
        reservas.saveAndFlush(reserva(pedido, Instant.now().plusSeconds(60)));

        assertThatThrownBy(() -> items.saveAndFlush(item(pedido, variante.getId(), 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ReservaRepository rechaza un ítem de una reserva que no existe")
    void itemSinReserva() {
        assertThatThrownBy(() -> items.saveAndFlush(item(UUID.randomUUID(), variante.getId(), 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ConfiguracionInventarioRepository tiene la semilla 5 y 5 y la fila se puede actualizar")
    void configuracionSemilla() {
        ConfiguracionInventario c = configuracion.findById(1L).orElseThrow();
        assertThat(c.getStockMinimoPorDefecto()).isEqualTo(5);
        assertThat(c.getMargenReservaMinutos()).isEqualTo(5);

        c.setStockMinimoPorDefecto(10);
        c.setMargenReservaMinutos(15);
        configuracion.saveAndFlush(c);

        assertThat(configuracion.findById(1L).orElseThrow().getMargenReservaMinutos()).isEqualTo(15);
    }

    @Test
    @DisplayName("CA-0a-06 ConfiguracionInventarioRepository rechaza un stock mínimo por defecto mayor a 999 (CHECK)")
    void minimoFueraDeRango() {
        ConfiguracionInventario c = configuracion.findById(1L).orElseThrow();
        c.setStockMinimoPorDefecto(1000);

        assertThatThrownBy(() -> configuracion.saveAndFlush(c)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ConfiguracionInventarioRepository rechaza un margen de reserva mayor a 30 minutos (CHECK)")
    void margenFueraDeRango() {
        ConfiguracionInventario c = configuracion.findById(1L).orElseThrow();
        c.setMargenReservaMinutos(31);

        assertThatThrownBy(() -> configuracion.saveAndFlush(c)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
