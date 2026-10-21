package com.fitstore.order.pedidos;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fitstore.common.event.SobreEvento;
import com.fitstore.order.event.BoletaEmitida;
import com.fitstore.order.event.EntregaActualizada;
import com.fitstore.order.event.PagoConfirmado;
import com.fitstore.order.event.PagoRechazado;
import com.fitstore.order.event.PersonaEvento;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pedidos.service.PedidoService;
import com.fitstore.order.pruebas.Datos;
import com.fitstore.order.pruebas.EntornoApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Saga del pedido con RabbitMQ real en Testcontainers: pago confirmado y rechazado, expiración, la carrera entre ambos,
 * boleta, eventos repetidos y las actualizaciones de la entrega (HU-4-05 y HU-4-09).
 */
@SpringBootTest
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = EntornoApi.class)
class SagaPedidoIT {

    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.3.5-management");
    private static final String EXCHANGE = "fitstore.eventos";
    private static final AtomicInteger NUMERO = new AtomicInteger(800000);

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void rabbit(DynamicPropertyRegistry registro) {
        registro.add("spring.rabbitmq.host", RABBIT::getHost);
        registro.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registro.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registro.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registro.add("fitstore.eventos.escuchar", () -> "true");
    }

    @Autowired PedidoRepository pedidos;
    @Autowired PedidoService servicio;
    @Autowired RabbitTemplate rabbit;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    @AfterEach
    void limpiar() {
        jdbc.execute("DELETE FROM pedido_historial");
        jdbc.execute("DELETE FROM pedido_item");
        jdbc.execute("DELETE FROM pedido");
        jdbc.execute("DELETE FROM outbox_evento");
        jdbc.execute("DELETE FROM evento_procesado");
    }

    // ---------- ayudas ----------

    private Pedido pedido(EstadoPedido estado, Modalidad modalidad) {
        String codigo = "FS-" + NUMERO.incrementAndGet();
        Pedido p = modalidad == Modalidad.RECOJO ? Datos.pedidoRecojo(codigo, UUID.randomUUID(), "k-" + codigo)
                : Datos.pedido(codigo, UUID.randomUUID(), "k-" + codigo);
        if (modalidad == Modalidad.AGENCIA) {
            p.setModalidad(Modalidad.AGENCIA);
        }
        p.setEstado(estado);
        return pedidos.saveAndFlush(p);
    }

    private UUID publicar(String tipo, String routingKey, Object datos) {
        return publicar(UUID.randomUUID(), tipo, routingKey, datos);
    }

    private UUID publicar(UUID eventoId, String tipo, String routingKey, Object datos) {
        rabbit.convertAndSend(EXCHANGE, routingKey,
                new SobreEvento(eventoId, tipo, 1, Instant.now(), json.valueToTree(datos)));
        return eventoId;
    }

    private PagoConfirmado pagoConfirmado(Pedido pedido) {
        return new PagoConfirmado(pedido.getId(), UUID.randomUUID(), UUID.randomUUID(), "SIMULADO", "TARJETA",
                pedido.getTotal(), Instant.now());
    }

    private String estado(Pedido p) {
        return jdbc.queryForObject("SELECT estado FROM pedido WHERE id = ?", String.class, p.getId());
    }

    private int historial(Pedido p) {
        return jdbc.queryForObject("SELECT count(*) FROM pedido_historial WHERE pedido_id = ?", Integer.class, p.getId());
    }

    private List<JsonNode> eventos(Pedido p, String tipo) {
        return jdbc.queryForList("SELECT payload::text FROM outbox_evento WHERE tipo = ? AND agregado_id = ?",
                String.class, tipo, p.getId().toString()).stream().map(json::readTree).toList();
    }

    private Map<String, Object> ultimoHistorial(Pedido p) {
        return jdbc.queryForMap("SELECT estado_anterior, estado_nuevo, actor_tipo, comentario FROM pedido_historial "
                + "WHERE pedido_id = ? ORDER BY id DESC LIMIT 1", p.getId());
    }

    // ---------- HU-4-05 ----------

    @Test
    @DisplayName("CA-4-27 PagoConfirmado pasa PENDIENTE_PAGO a PAGADO, lo anota en el historial (SISTEMA) y publica PedidoPagado")
    void pagoConfirmado() {
        Pedido p = pedido(EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);

        publicar("PagoConfirmado", "payment.pago.confirmado", pagoConfirmado(p));

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(estado(p)).isEqualTo("PAGADO"));
        assertThat(ultimoHistorial(p)).containsEntry("estado_anterior", "PENDIENTE_PAGO")
                .containsEntry("estado_nuevo", "PAGADO").containsEntry("actor_tipo", "SISTEMA");
        assertThat(jdbc.queryForMap("SELECT pago_proveedor, pago_medio FROM pedido WHERE id = ?", p.getId()))
                .containsEntry("pago_proveedor", "SIMULADO").containsEntry("pago_medio", "TARJETA");
        List<JsonNode> pagados = eventos(p, "PedidoPagado");
        assertThat(pagados).hasSize(1);
        assertThat(pagados.get(0).get("codigo").asString()).isEqualTo(p.getCodigo());
        assertThat(pagados.get(0).get("items")).hasSize(2);
    }

    @Test
    @DisplayName("CA-4-28 PagoRechazado no cambia el estado, anota \"Pago rechazado: motivo\" y el cliente puede reintentar")
    void pagoRechazado() {
        Pedido p = pedido(EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);

        publicar("PagoRechazado", "payment.pago.rechazado", new PagoRechazado(p.getId(), "SIMULADO", "Fondos insuficientes"));

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(historial(p)).isEqualTo(2));
        assertThat(estado(p)).isEqualTo("PENDIENTE_PAGO");
        assertThat(ultimoHistorial(p)).containsEntry("comentario", "Pago rechazado: Fondos insuficientes");

        publicar("PagoConfirmado", "payment.pago.confirmado", pagoConfirmado(p));
        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(estado(p)).isEqualTo("PAGADO"));
    }

    @Test
    @DisplayName("CA-4-29 el expirador pasa a EXPIRADO los pendientes vencidos y publica PedidoExpirado")
    void expiracion() {
        Pedido vencido = pedido(EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);
        Pedido vigente = pedido(EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);
        jdbc.update("UPDATE pedido SET expira_en = ? WHERE id = ?", java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)),
                vencido.getId());

        int expirados = servicio.expirarVencidos();

        assertThat(expirados).isEqualTo(1);
        assertThat(estado(vencido)).isEqualTo("EXPIRADO");
        assertThat(ultimoHistorial(vencido)).containsEntry("actor_tipo", "SISTEMA");
        assertThat(eventos(vencido, "PedidoExpirado")).hasSize(1);
        assertThat(estado(vigente)).isEqualTo("PENDIENTE_PAGO");
        assertThat(eventos(vigente, "PedidoExpirado")).isEmpty();
    }

    @Test
    @DisplayName("CA-4-30 si la expiración ganó, PagoConfirmado no cambia el pedido y publica PagoNoAplicable")
    void ganaLaExpiracion() {
        Pedido p = pedido(EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);
        jdbc.update("UPDATE pedido SET expira_en = ? WHERE id = ?", java.sql.Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)),
                p.getId());
        servicio.expirarVencidos();
        PagoConfirmado pago = pagoConfirmado(p);

        publicar("PagoConfirmado", "payment.pago.confirmado", pago);

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(eventos(p, "PagoNoAplicable")).hasSize(1));
        assertThat(estado(p)).isEqualTo("EXPIRADO");
        JsonNode evento = eventos(p, "PagoNoAplicable").get(0);
        assertThat(evento.get("pagoId").asString()).isEqualTo(pago.intencionId().toString());
        assertThat(evento.get("motivo").asString()).isEqualTo("PEDIDO_EXPIRADO");
        assertThat(eventos(p, "PedidoPagado")).isEmpty();
    }

    @Test
    @DisplayName("CA-4-30 si el pago ganó, la expiración tardía no cambia nada ni publica PedidoExpirado")
    void ganaElPago() {
        Pedido p = pedido(EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);
        publicar("PagoConfirmado", "payment.pago.confirmado", pagoConfirmado(p));
        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(estado(p)).isEqualTo("PAGADO"));

        boolean expiro = servicio.expirar(p.getId());

        assertThat(expiro).isFalse();
        assertThat(estado(p)).isEqualTo("PAGADO");
        assertThat(eventos(p, "PedidoExpirado")).isEmpty();
    }

    @Test
    @DisplayName("CA-4-30 un pago que llega a un pedido cancelado publica PagoNoAplicable con PEDIDO_CANCELADO")
    void pagoSobreCancelado() {
        Pedido p = pedido(EstadoPedido.CANCELADO, Modalidad.DELIVERY_LOCAL);

        publicar("PagoConfirmado", "payment.pago.confirmado", pagoConfirmado(p));

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(eventos(p, "PagoNoAplicable")).hasSize(1));
        assertThat(eventos(p, "PagoNoAplicable").get(0).get("motivo").asString()).isEqualTo("PEDIDO_CANCELADO");
        assertThat(estado(p)).isEqualTo("CANCELADO");
    }

    @Test
    @DisplayName("CA-4-31 BoletaEmitida guarda boletaUrl en el pedido")
    void boleta() {
        Pedido p = pedido(EstadoPedido.PAGADO, Modalidad.DELIVERY_LOCAL);

        publicar("BoletaEmitida", "payment.boleta.emitida",
                new BoletaEmitida(p.getId(), "B001-000123", "/api/v1/pagos/" + p.getCodigo() + "/boleta"));

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(
                jdbc.queryForObject("SELECT boleta_url FROM pedido WHERE id = ?", String.class, p.getId()))
                .isEqualTo("/api/v1/pagos/" + p.getCodigo() + "/boleta"));
    }

    @Test
    @DisplayName("CA-4-32 el mismo evento dos veces se procesa una sola vez (evento_procesado)")
    void eventoRepetido() {
        Pedido p = pedido(EstadoPedido.PENDIENTE_PAGO, Modalidad.DELIVERY_LOCAL);
        PagoConfirmado pago = pagoConfirmado(p);
        UUID eventoId = UUID.randomUUID();

        publicar(eventoId, "PagoConfirmado", "payment.pago.confirmado", pago);
        publicar(eventoId, "PagoConfirmado", "payment.pago.confirmado", pago);
        // La cola es ordenada: cuando se procesa esta boleta, los dos pagos anteriores ya pasaron
        publicar("BoletaEmitida", "payment.boleta.emitida", new BoletaEmitida(p.getId(), "B001-1", "/boleta"));

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(
                jdbc.queryForObject("SELECT boleta_url FROM pedido WHERE id = ?", String.class, p.getId())).isEqualTo("/boleta"));
        assertThat(estado(p)).isEqualTo("PAGADO");
        assertThat(eventos(p, "PedidoPagado")).hasSize(1);
        assertThat(historial(p)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evento_procesado WHERE evento_id = ?", Integer.class, eventoId))
                .isEqualTo(1);
    }

    // ---------- HU-4-09: EntregaActualizada ----------

    private EntregaActualizada entrega(Pedido p, String estado, String agencia, String seguimiento, String observacion) {
        return new EntregaActualizada(UUID.randomUUID(), p.getId(), p.getCodigo(), p.getModalidad().name(), estado, 1,
                null, null, agencia, seguimiento, null, null, observacion, new PersonaEvento("Ana Quispe", "ana@correo.pe"));
    }

    @Test
    @DisplayName("CA-4-45 EntregaActualizada \"en ruta\" pasa LISTO_DESPACHO a ENVIADO y \"entregada\" a ENTREGADO")
    void enRutaYEntregada() {
        Pedido p = pedido(EstadoPedido.LISTO_DESPACHO, Modalidad.DELIVERY_LOCAL);

        publicar("EntregaActualizada", "delivery.entrega.actualizada", entrega(p, "EN_RUTA", null, null, null));
        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(estado(p)).isEqualTo("ENVIADO"));
        assertThat(ultimoHistorial(p)).containsEntry("actor_tipo", "SISTEMA");

        publicar("EntregaActualizada", "delivery.entrega.actualizada", entrega(p, "ENTREGADA", null, null, null));
        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(estado(p)).isEqualTo("ENTREGADO"));
    }

    @Test
    @DisplayName("CA-4-45 \"enviado a agencia\" pasa a ENVIADO y guarda la agencia y el código de seguimiento")
    void enviadoAAgencia() {
        Pedido p = pedido(EstadoPedido.LISTO_DESPACHO, Modalidad.AGENCIA);

        publicar("EntregaActualizada", "delivery.entrega.actualizada", entrega(p, "ENVIADO_AGENCIA", "Shalom", "SH-123", null));

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(estado(p)).isEqualTo("ENVIADO"));
        assertThat(jdbc.queryForMap("SELECT agencia_nombre, codigo_seguimiento FROM pedido WHERE id = ?", p.getId()))
                .containsEntry("agencia_nombre", "Shalom").containsEntry("codigo_seguimiento", "SH-123");
        assertThat((String) ultimoHistorial(p).get("comentario")).contains("Shalom").contains("SH-123");
    }

    @Test
    @DisplayName("CA-4-45 \"fallida\" deja el pedido ENVIADO y anota el intento en el historial")
    void intentoFallido() {
        Pedido p = pedido(EstadoPedido.ENVIADO, Modalidad.DELIVERY_LOCAL);
        int antes = historial(p);

        publicar("EntregaActualizada", "delivery.entrega.actualizada", entrega(p, "FALLIDA", null, null, "Cliente ausente"));

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(historial(p)).isEqualTo(antes + 1));
        assertThat(estado(p)).isEqualTo("ENVIADO");
        assertThat((String) ultimoHistorial(p).get("comentario")).startsWith("Intento fallido").contains("Cliente ausente");
    }

    @Test
    @DisplayName("CA-4-45 \"devuelta\" pasa ENVIADO a DEVUELTO (final) y guarda el motivo en el historial")
    void devuelta() {
        Pedido p = pedido(EstadoPedido.ENVIADO, Modalidad.AGENCIA);

        publicar("EntregaActualizada", "delivery.entrega.actualizada", entrega(p, "DEVUELTA", null, null, "Rechazado en agencia"));

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(estado(p)).isEqualTo("DEVUELTO"));
        assertThat(ultimoHistorial(p)).containsEntry("comentario", "Rechazado en agencia");
    }

    @Test
    @DisplayName("CA-4-45 las etapas de delivery que no cambian el pedido (asignada, pendiente) se ignoran")
    void etapasQueSeIgnoran() {
        Pedido p = pedido(EstadoPedido.LISTO_DESPACHO, Modalidad.DELIVERY_LOCAL);

        publicar("EntregaActualizada", "delivery.entrega.actualizada", entrega(p, "ASIGNADA", null, null, null));
        publicar("BoletaEmitida", "payment.boleta.emitida", new BoletaEmitida(p.getId(), "B001-9", "/b"));

        await().atMost(20, SECONDS).untilAsserted(() -> assertThat(
                jdbc.queryForObject("SELECT boleta_url FROM pedido WHERE id = ?", String.class, p.getId())).isEqualTo("/b"));
        assertThat(estado(p)).isEqualTo("LISTO_DESPACHO");
    }
}
