package com.fitstore.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.order.pedidos.model.Destinatario;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.repository.ContadoresPedidos;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pedidos.repository.PedidoSpecifications;
import com.fitstore.order.pruebas.Datos;
import com.fitstore.order.pruebas.PruebaRepositorio;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** Filtros y contadores del listado del backoffice (HU-4-08): cada filtro solo y combinados. */
@PruebaRepositorio
@Import(ContadoresPedidos.class)
class PedidoSpecificationsTest {

    @Autowired PedidoRepository repositorio;
    @Autowired ContadoresPedidos contadores;
    @Autowired TestEntityManager em;
    @Autowired JdbcTemplate jdbc;

    private static final AtomicInteger NUMERO = new AtomicInteger(5000);
    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();
    private final Instant ahora = Instant.now();

    private Pedido pedido(UUID cliente, EstadoPedido estado, Modalidad modalidad, String nombres, String dni, String email,
            long haceDias) {
        String codigo = "FS-" + String.format("%06d", NUMERO.incrementAndGet());
        Pedido p = modalidad == Modalidad.RECOJO ? Datos.pedidoRecojo(codigo, cliente, "k-" + codigo)
                : Datos.pedido(codigo, cliente, "k-" + codigo);
        p.setModalidad(modalidad);
        p.setEstado(estado);
        p.setDestinatario(new Destinatario(nombres, "Quispe", dni, "987654321", email));
        p = repositorio.saveAndFlush(p);
        jdbc.update("UPDATE pedido SET creado_en = ? WHERE id = ?",
                java.sql.Timestamp.from(ahora.minus(haceDias, ChronoUnit.DAYS)), p.getId());
        return p;
    }

    private Pedido anaPagado;
    private Pedido anaPendiente;
    private Pedido luisPagadoRecojo;
    private Pedido luisEntregado;

    @BeforeEach
    void datos() {
        anaPagado = pedido(ana, EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL, "Ana", "11111111", "ana@correo.pe", 5);
        anaPendiente = pedido(ana, EstadoPedido.PENDIENTE_PAGO, Modalidad.AGENCIA, "Ana", "11111111", "ana@correo.pe", 1);
        luisPagadoRecojo = pedido(luis, EstadoPedido.PAGADO, Modalidad.RECOJO, "Luis", "22222222", "luis@mail.com", 3);
        luisEntregado = pedido(luis, EstadoPedido.ENTREGADO, Modalidad.DELIVERY_LOCAL, "Luis", "22222222", "luis@mail.com", 20);
        em.clear();
    }

    private List<Pedido> buscar(EstadoPedido estado, Modalidad modalidad, Instant desde, Instant hasta, String codigo,
            String cliente, UUID clienteId) {
        return repositorio.findAll(PedidoSpecifications.filtrar(estado, modalidad, desde, hasta, codigo, cliente, clienteId));
    }

    @Test
    @DisplayName("CA-4-41 filtra por estado")
    void porEstado() {
        assertThat(buscar(EstadoPedido.PAGADO, null, null, null, null, null, null))
                .extracting(Pedido::getId).containsExactlyInAnyOrder(anaPagado.getId(), luisPagadoRecojo.getId());
    }

    @Test
    @DisplayName("CA-4-41 filtra por modalidad")
    void porModalidad() {
        assertThat(buscar(null, Modalidad.RECOJO, null, null, null, null, null))
                .extracting(Pedido::getId).containsExactly(luisPagadoRecojo.getId());
    }

    @Test
    @DisplayName("CA-4-41 filtra por rango de fechas de creación (desde incluido, hasta excluido)")
    void porFechas() {
        assertThat(buscar(null, null, ahora.minus(6, ChronoUnit.DAYS), ahora.minus(2, ChronoUnit.DAYS), null, null, null))
                .extracting(Pedido::getId).containsExactlyInAnyOrder(anaPagado.getId(), luisPagadoRecojo.getId());
        assertThat(buscar(null, null, ahora.minus(2, ChronoUnit.DAYS), null, null, null, null))
                .extracting(Pedido::getId).containsExactly(anaPendiente.getId());
    }

    @Test
    @DisplayName("CA-4-41 filtra por código (completo o parte)")
    void porCodigo() {
        assertThat(buscar(null, null, null, null, anaPagado.getCodigo(), null, null))
                .extracting(Pedido::getId).containsExactly(anaPagado.getId());
        assertThat(buscar(null, null, null, null, anaPagado.getCodigo().toLowerCase(), null, null)).hasSize(1);
    }

    @Test
    @DisplayName("CA-4-41 filtra por cliente: nombre, correo o DNI")
    void porCliente() {
        assertThat(buscar(null, null, null, null, null, "ana", null)).hasSize(2);
        assertThat(buscar(null, null, null, null, null, "LUIS@MAIL", null)).hasSize(2);
        assertThat(buscar(null, null, null, null, null, "22222222", null)).hasSize(2);
        assertThat(buscar(null, null, null, null, null, "Quispe", null)).hasSize(4);
        assertThat(buscar(null, null, null, null, null, "nadie", null)).isEmpty();
    }

    @Test
    @DisplayName("CA-4-42 filtra por el id del cliente (historial de un cliente)")
    void porClienteId() {
        assertThat(buscar(null, null, null, null, null, null, luis))
                .extracting(Pedido::getId).containsExactlyInAnyOrder(luisPagadoRecojo.getId(), luisEntregado.getId());
    }

    @Test
    @DisplayName("CA-4-41 los filtros se combinan: todos deben cumplirse")
    void combinados() {
        assertThat(buscar(EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL, null, null, null, "ana", null))
                .extracting(Pedido::getId).containsExactly(anaPagado.getId());
        assertThat(buscar(EstadoPedido.PAGADO, Modalidad.RECOJO, null, null, null, "ana", null)).isEmpty();
        assertThat(buscar(EstadoPedido.PAGADO, null, null, null, null, null, ana))
                .extracting(Pedido::getId).containsExactly(anaPagado.getId());
    }

    @Test
    @DisplayName("CA-4-40 los más antiguos por preparar van primero, antes de los pedidos que ya no requieren acción")
    void ordenDePreparacion() {
        var pagina = repositorio.findAll(PedidoSpecifications.filtrar(null, null, null, null, null, null, null)
                .and(PedidoSpecifications.prioridadDePreparacion()), PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Pedido::getId).containsExactly(
                anaPagado.getId(),          // PAGADO, hace 5 días
                luisPagadoRecojo.getId(),   // PAGADO, hace 3 días
                luisEntregado.getId(),      // ya no requieren acción, del más antiguo
                anaPendiente.getId());
    }

    @Test
    @DisplayName("CA-4-40 el contador por estado ignora el filtro de estado pero respeta los demás")
    void contadoresPorEstado() {
        Map<EstadoPedido, Long> todos = contadores.porEstado(PedidoSpecifications.filtrar(null, null, null, null, null, null, null));
        assertThat(todos.get(EstadoPedido.PAGADO)).isEqualTo(2);
        assertThat(todos.get(EstadoPedido.PENDIENTE_PAGO)).isEqualTo(1);
        assertThat(todos.get(EstadoPedido.ENTREGADO)).isEqualTo(1);
        assertThat(todos.get(EstadoPedido.CANCELADO)).isZero();
        assertThat(todos).hasSize(EstadoPedido.values().length);

        Map<EstadoPedido, Long> deAna = contadores.porEstado(PedidoSpecifications.filtrar(null, null, null, null, null, null, ana));
        assertThat(deAna.get(EstadoPedido.PAGADO)).isEqualTo(1);
        assertThat(deAna.get(EstadoPedido.ENTREGADO)).isZero();
    }
}
