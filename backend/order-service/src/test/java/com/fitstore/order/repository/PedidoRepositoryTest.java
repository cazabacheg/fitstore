package com.fitstore.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.model.TipoActor;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pedidos.repository.PedidoSpecifications;
import com.fitstore.order.pruebas.Datos;
import com.fitstore.order.pruebas.PruebaRepositorio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/** CA-0a-06: capa de datos de {@code PedidoRepository} (los pedidos nunca se eliminan: invariante 7). */
@PruebaRepositorio
class PedidoRepositoryTest {

    @Autowired PedidoRepository repositorio;
    @Autowired TestEntityManager em;
    @Autowired JdbcTemplate jdbc;

    private final UUID cliente = UUID.randomUUID();

    private Pedido guardar(Pedido pedido) {
        repositorio.saveAndFlush(pedido);
        em.clear();
        return repositorio.findById(pedido.getId()).orElseThrow();
    }

    @Test
    @DisplayName("CA-0a-06 inserta un pedido con sus ítems e historial en cascada")
    void insertaConCascada() {
        Pedido guardado = guardar(Datos.pedido("FS-000001", cliente, "k-1"));

        assertThat(guardado.getVersion()).isZero();
        assertThat(guardado.getCreadoEn()).isNotNull();
        assertThat(guardado.getActualizadoEn()).isNotNull();
        assertThat(guardado.getMontoReembolsado()).isEqualByComparingTo("0");
        assertThat(guardado.getDestinatario().getDni()).isEqualTo("12345678");
        assertThat(guardado.getDireccion().getDistrito()).isEqualTo("Lima");
        assertThat(guardado.getEnvio().getPlazoDiasMax()).isEqualTo(2);
        assertThat(guardado.getItems()).hasSize(2)
                .extracting("sku").containsExactly("POL-NEG-M", "POL-BLA-L");
        assertThat(guardado.getHistorial()).hasSize(1);
        assertThat(guardado.getHistorial().get(0).getEstadoAnterior()).isNull();
        assertThat(guardado.getHistorial().get(0).getEstadoNuevo()).isEqualTo(EstadoPedido.PENDIENTE_PAGO);
        assertThat(guardado.getHistorial().get(0).getActorTipo()).isEqualTo(TipoActor.CLIENTE);
    }

    @Test
    @DisplayName("CA-0a-06 un pedido de recojo se guarda sin dirección ni envío")
    void recojoSinDireccion() {
        Pedido guardado = guardar(Datos.pedidoRecojo("FS-000002", cliente, "k-2"));

        assertThat(guardado.getModalidad()).isEqualTo(Modalidad.RECOJO);
        assertThat(guardado.getDireccion()).isNull();
        assertThat(guardado.getEnvio()).isNull();
    }

    @Test
    @DisplayName("CA-0a-06 busca por código y por cliente con clave de idempotencia")
    void buscaPorCodigoEIdempotencia() {
        guardar(Datos.pedido("FS-000003", cliente, "k-3"));

        assertThat(repositorio.findByCodigo("FS-000003")).isPresent();
        assertThat(repositorio.findByCodigo("FS-999999")).isEmpty();
        assertThat(repositorio.findByClienteIdAndIdempotencyKey(cliente, "k-3")).isPresent();
        assertThat(repositorio.findByClienteIdAndIdempotencyKey(UUID.randomUUID(), "k-3")).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 la transición condicional de estado la gana una sola llamada")
    void transicionCondicional() {
        Pedido pedido = guardar(Datos.pedido("FS-000004", cliente, "k-4"));

        int primera = repositorio.cambiarEstadoSi(pedido.getId(), EstadoPedido.PENDIENTE_PAGO, EstadoPedido.PAGADO);
        int segunda = repositorio.cambiarEstadoSi(pedido.getId(), EstadoPedido.PENDIENTE_PAGO, EstadoPedido.EXPIRADO);

        assertThat(primera).isEqualTo(1);
        assertThat(segunda).isZero();
        em.clear();
        Pedido recargado = repositorio.findById(pedido.getId()).orElseThrow();
        assertThat(recargado.getEstado()).isEqualTo(EstadoPedido.PAGADO);
        assertThat(recargado.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-06 actualiza los datos del pago desde PagoConfirmado")
    void actualizaDatosDelPago() {
        Pedido pedido = guardar(Datos.pedido("FS-000005", cliente, "k-5"));
        pedido.setPagoProveedor("CULQI");
        pedido.setPagoMedio("TARJETA");
        pedido.setPagadoEn(Instant.parse("2026-10-19T15:00:00Z"));
        pedido.setBoletaUrl("/api/v1/pagos/FS-000005/boleta");
        pedido.setMontoReembolsado(new BigDecimal("20.00"));
        repositorio.saveAndFlush(pedido);
        em.clear();

        Pedido recargado = repositorio.findById(pedido.getId()).orElseThrow();
        assertThat(recargado.getPagoProveedor()).isEqualTo("CULQI");
        assertThat(recargado.getPagoMedio()).isEqualTo("TARJETA");
        assertThat(recargado.getPagadoEn()).isEqualTo(Instant.parse("2026-10-19T15:00:00Z"));
        assertThat(recargado.getBoletaUrl()).endsWith("/boleta");
        assertThat(recargado.getMontoReembolsado()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("CA-0a-06 lista los pedidos de un cliente, el más reciente primero")
    void listaPorCliente() {
        Pedido viejo = Datos.pedido("FS-000006", cliente, "k-6");
        viejo.setCreadoEn(Instant.now().minus(2, ChronoUnit.DAYS));
        Pedido nuevo = Datos.pedido("FS-000007", cliente, "k-7");
        nuevo.setCreadoEn(Instant.now());
        repositorio.saveAndFlush(viejo);
        repositorio.saveAndFlush(nuevo);
        repositorio.saveAndFlush(Datos.pedido("FS-000008", UUID.randomUUID(), "k-8"));

        var pagina = repositorio.findByClienteId(cliente,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "creadoEn")));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent()).extracting(Pedido::getCodigo).containsExactly("FS-000007", "FS-000006");
    }

    @Test
    @DisplayName("CA-0a-06 lista con los filtros del backoffice")
    void listaConFiltros() {
        Pedido a = Datos.pedido("FS-000010", cliente, "f-1");
        a.setCreadoEn(Instant.parse("2026-10-01T10:00:00Z"));
        Pedido b = Datos.pedidoRecojo("FS-000011", cliente, "f-2");
        b.setEstado(EstadoPedido.PAGADO);
        b.setCreadoEn(Instant.parse("2026-10-10T10:00:00Z"));
        Pedido c = Datos.pedido("FS-000012", UUID.randomUUID(), "f-3");
        c.setCreadoEn(Instant.parse("2026-10-15T10:00:00Z"));
        repositorio.saveAllAndFlush(java.util.List.of(a, b, c));
        var todos = PageRequest.of(0, 10);

        assertThat(codigos(PedidoSpecifications.filtrar(null, null, null, null, null, null), todos)).hasSize(3);
        assertThat(codigos(PedidoSpecifications.filtrar(EstadoPedido.PAGADO, null, null, null, null, null), todos))
                .containsExactly("FS-000011");
        assertThat(codigos(PedidoSpecifications.filtrar(null, Modalidad.DELIVERY_LOCAL, null, null, null, null), todos))
                .containsExactlyInAnyOrder("FS-000010", "FS-000012");
        assertThat(codigos(PedidoSpecifications.filtrar(null, null, Instant.parse("2026-10-05T00:00:00Z"),
                Instant.parse("2026-10-12T00:00:00Z"), null, null), todos)).containsExactly("FS-000011");
        assertThat(codigos(PedidoSpecifications.filtrar(null, null, null, null, "000012", null), todos))
                .containsExactly("FS-000012");
        assertThat(codigos(PedidoSpecifications.filtrar(null, null, null, null, null, cliente), todos))
                .containsExactlyInAnyOrder("FS-000010", "FS-000011");
    }

    private java.util.List<String> codigos(org.springframework.data.jpa.domain.Specification<Pedido> filtro,
            org.springframework.data.domain.Pageable pagina) {
        return repositorio.findAll(filtro, pagina).map(Pedido::getCodigo).getContent();
    }

    @Test
    @DisplayName("CA-0a-06 lista los pedidos vencidos: pendientes de pago cuyo plazo ya pasó")
    void listaVencidos() {
        Pedido vencido = Datos.pedido("FS-000020", cliente, "v-1");
        vencido.setExpiraEn(Instant.now().minus(5, ChronoUnit.MINUTES));
        Pedido vigente = Datos.pedido("FS-000021", cliente, "v-2");
        Pedido pagado = Datos.pedido("FS-000022", cliente, "v-3");
        pagado.setEstado(EstadoPedido.PAGADO);
        pagado.setExpiraEn(Instant.now().minus(5, ChronoUnit.MINUTES));
        repositorio.saveAllAndFlush(java.util.List.of(vencido, vigente, pagado));

        var vencidos = repositorio.findByEstadoAndExpiraEnBefore(EstadoPedido.PENDIENTE_PAGO, Instant.now());

        assertThat(vencidos).extracting(Pedido::getCodigo).containsExactly("FS-000020");
    }

    @Test
    @DisplayName("CA-0a-06 la secuencia del código crece de uno en uno")
    void secuenciaDelCodigo() {
        long primero = repositorio.siguienteNumeroCodigo();
        long segundo = repositorio.siguienteNumeroCodigo();

        assertThat(segundo).isEqualTo(primero + 1);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza dos pedidos con la misma clave de idempotencia del mismo cliente")
    void idempotenciaDuplicada() {
        repositorio.saveAndFlush(Datos.pedido("FS-000030", cliente, "dup"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.pedido("FS-000031", cliente, "dup")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 permite la misma clave de idempotencia en clientes distintos")
    void idempotenciaDeOtroCliente() {
        repositorio.saveAndFlush(Datos.pedido("FS-000032", cliente, "misma"));
        repositorio.saveAndFlush(Datos.pedido("FS-000033", UUID.randomUUID(), "misma"));

        assertThat(repositorio.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un código de pedido repetido")
    void codigoDuplicado() {
        repositorio.saveAndFlush(Datos.pedido("FS-000034", cliente, "c-1"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.pedido("FS-000034", UUID.randomUUID(), "c-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: rechaza un total negativo")
    void montoNegativo() {
        Pedido pedido = Datos.pedido("FS-000035", cliente, "n-1");
        pedido.setTotal(new BigDecimal("-1.00"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(pedido)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: rechaza un costo de envío negativo")
    void envioNegativo() {
        Pedido pedido = Datos.pedido("FS-000036", cliente, "n-2");
        pedido.setCostoEnvio(new BigDecimal("-0.01"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(pedido)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: rechaza un ítem con cantidad cero")
    void cantidadCero() {
        Pedido pedido = Datos.pedido("FS-000037", cliente, "q-1");
        pedido.getItems().get(0).setCantidad(0);

        assertThatThrownBy(() -> repositorio.saveAndFlush(pedido)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: rechaza un estado que no existe")
    void estadoInvalido() {
        repositorio.saveAndFlush(Datos.pedido("FS-000038", cliente, "e-1"));

        assertThatThrownBy(() -> jdbc.update("UPDATE pedido SET estado = 'INEXISTENTE' WHERE codigo = 'FS-000038'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: rechaza una modalidad que no existe")
    void modalidadInvalida() {
        repositorio.saveAndFlush(Datos.pedido("FS-000039", cliente, "m-1"));

        assertThatThrownBy(() -> jdbc.update("UPDATE pedido SET modalidad = 'DRON' WHERE codigo = 'FS-000039'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: rechaza un tipo de actor que no existe en el historial")
    void actorInvalido() {
        Pedido pedido = guardar(Datos.pedido("FS-000040", cliente, "a-1"));

        assertThatThrownBy(() -> jdbc.update("UPDATE pedido_historial SET actor_tipo = 'ROBOT' WHERE pedido_id = ?",
                pedido.getId())).isInstanceOf(DataIntegrityViolationException.class);
    }
}
