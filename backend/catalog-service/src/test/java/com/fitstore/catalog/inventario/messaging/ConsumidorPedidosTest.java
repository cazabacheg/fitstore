package com.fitstore.catalog.inventario.messaging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fitstore.catalog.inventario.dto.ItemReservaDto;
import com.fitstore.catalog.inventario.dto.ReservaRequest;
import com.fitstore.catalog.inventario.service.ReservaService;
import com.fitstore.catalog.pruebas.DatosConfirmados;
import com.fitstore.catalog.pruebas.PostgresPrueba;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
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
 * Consumidores de {@code PedidoPagado}, {@code PedidoCancelado} y {@code PedidoExpirado} con RabbitMQ real en
 * Testcontainers, y el liberador de reservas vencidas (HU-2-10: CA-2-49 a CA-2-53).
 */
@SpringBootTest
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
class ConsumidorPedidosTest {

    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.3.5-management");
    private static final String EXCHANGE = "fitstore.eventos";
    private static final String COLA_ALERTAS = "prueba.alertas";

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
        registro.add("fitstore.outbox.intervalo-ms", () -> "200");
    }

    @Autowired ReservaService reservas;
    @Autowired LiberadorReservasVencidas liberador;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired AmqpAdmin admin;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    DatosConfirmados datos;

    @BeforeEach
    void preparar() {
        datos = new DatosConfirmados(jdbc);
        TopicExchange exchange = new TopicExchange(EXCHANGE, true, false);
        admin.declareExchange(exchange);
        Queue alertas = new Queue(COLA_ALERTAS, true, false, false);
        admin.declareQueue(alertas);
        admin.declareBinding(BindingBuilder.bind(alertas).to(exchange).with("catalog.stock.#"));
        admin.purgeQueue(COLA_ALERTAS, true);
    }

    @AfterEach
    void limpiar() {
        datos.limpiar();
    }

    // ------------------------------------------------------------ utilidades

    private UUID reservar(long variante, int cantidad) {
        UUID pedido = UUID.randomUUID();
        reservas.reservar(new ReservaRequest(pedido, Instant.now().plusSeconds(1800),
                List.of(new ItemReservaDto(variante, cantidad))));
        return pedido;
    }

    private UUID publicar(String routingKey, String tipo, String datosJson) {
        return publicar(UUID.randomUUID(), routingKey, tipo, datosJson);
    }

    private UUID publicar(UUID eventoId, String routingKey, String tipo, String datosJson) {
        String sobre = "{\"eventoId\":\"" + eventoId + "\",\"tipo\":\"" + tipo + "\",\"version\":1,\"ocurridoEn\":\""
                + Instant.now() + "\",\"datos\":" + datosJson + "}";
        Message mensaje = MessageBuilder.withBody(sobre.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .setContentType("application/json").build();
        rabbitTemplate.send(EXCHANGE, routingKey, mensaje);
        return eventoId;
    }

    private UUID pagado(UUID pedido, String codigo, long variante, int cantidad) {
        return publicar("order.pedido.pagado", "PedidoPagado", "{\"pedidoId\":\"" + pedido + "\",\"codigo\":\"" + codigo
                + "\",\"items\":[{\"varianteId\":" + variante + ",\"cantidad\":" + cantidad + "}]}");
    }

    private UUID cancelado(UUID pedido, String codigo, boolean fuePagado) {
        return publicar("order.pedido.cancelado", "PedidoCancelado", "{\"pedidoId\":\"" + pedido + "\",\"codigo\":\"" + codigo
                + "\",\"fuePagado\":" + fuePagado + ",\"motivo\":null,\"actor\":\"CLIENTE\",\"items\":[]}");
    }

    private UUID expirado(UUID pedido, String codigo) {
        return publicar("order.pedido.expirado", "PedidoExpirado", "{\"pedidoId\":\"" + pedido + "\",\"codigo\":\"" + codigo + "\"}");
    }

    private void esperarProcesado(UUID eventoId) {
        await().atMost(10, SECONDS).pollInterval(100, java.util.concurrent.TimeUnit.MILLISECONDS).until(() ->
                jdbc.queryForObject("SELECT count(*) FROM evento_procesado WHERE evento_id = ?::uuid", Integer.class,
                        eventoId.toString()) == 1);
    }

    private JsonNode esperarAlerta(String tipo) throws Exception {
        for (int i = 0; i < 40; i++) {
            Message mensaje = rabbitTemplate.receive(COLA_ALERTAS, 500);
            if (mensaje != null) {
                JsonNode sobre = json.readTree(new String(mensaje.getBody(), java.nio.charset.StandardCharsets.UTF_8));
                if (tipo.equals(sobre.path("tipo").asText())) {
                    return sobre;
                }
            }
        }
        throw new AssertionError("No llegó el evento " + tipo + " a RabbitMQ");
    }

    private Object movimiento(long variante, String tipo, String columna) {
        return jdbc.queryForObject("SELECT " + columna + " FROM movimiento_stock WHERE variante_id = ? AND tipo = ?",
                Object.class, variante, tipo);
    }

    // ------------------------------------------------------------ CA-2-49

    @Test
    @DisplayName("CA-2-49 PedidoPagado con la reserva activa baja el físico y el reservado, la deja CONFIRMADA y registra un movimiento VENTA")
    void pagoConfirmaLaReserva() {
        long variante = datos.variante("EVT01", 10, 0);
        UUID pedido = reservar(variante, 3);

        esperarProcesado(pagado(pedido, "FS-000101", variante, 3));

        assertThat(datos.fisico(variante)).isEqualTo(7);
        assertThat(datos.reservado(variante)).isZero();
        assertThat(datos.estadoReserva(pedido)).isEqualTo("CONFIRMADA");
        assertThat(datos.movimientos(variante, "VENTA")).isEqualTo(1);
        assertThat(movimiento(variante, "VENTA", "cantidad")).isEqualTo(-3);
        assertThat(movimiento(variante, "VENTA", "saldo_fisico")).isEqualTo(7);
        assertThat(movimiento(variante, "VENTA", "referencia")).isEqualTo("FS-000101");
        assertThat(movimiento(variante, "VENTA", "usuario_id")).isNull();
    }

    // ------------------------------------------------------------ CA-2-50

    @Test
    @DisplayName("CA-2-50 PedidoCancelado y PedidoExpirado con la reserva activa liberan lo reservado sin tocar el físico")
    void cancelarOExpirarLibera() {
        long variante = datos.variante("EVT02", 10, 0);
        UUID cancelada = reservar(variante, 2);
        UUID expirada = reservar(variante, 3);
        assertThat(datos.reservado(variante)).isEqualTo(5);

        esperarProcesado(cancelado(cancelada, "FS-000102", false));
        assertThat(datos.estadoReserva(cancelada)).isEqualTo("LIBERADA");
        assertThat(datos.reservado(variante)).isEqualTo(3);

        esperarProcesado(expirado(expirada, "FS-000103"));
        assertThat(datos.estadoReserva(expirada)).isEqualTo("LIBERADA");
        assertThat(datos.reservado(variante)).isZero();
        assertThat(datos.fisico(variante)).isEqualTo(10);
        assertThat(datos.movimientos(variante, "VENTA") + datos.movimientos(variante, "REINGRESO")).isZero();
    }

    @Test
    @DisplayName("CA-2-50 PedidoCancelado de un pedido ya pagado (reserva CONFIRMADA) devuelve el stock al físico con un movimiento REINGRESO")
    void cancelarUnPedidoPagado() {
        long variante = datos.variante("EVT03", 10, 0);
        UUID pedido = reservar(variante, 3);
        esperarProcesado(pagado(pedido, "FS-000104", variante, 3));
        assertThat(datos.fisico(variante)).isEqualTo(7);

        esperarProcesado(cancelado(pedido, "FS-000104", true));

        assertThat(datos.fisico(variante)).isEqualTo(10);
        assertThat(datos.reservado(variante)).isZero();
        assertThat(datos.estadoReserva(pedido)).isEqualTo("REINGRESADA");
        assertThat(movimiento(variante, "REINGRESO", "cantidad")).isEqualTo(3);
        assertThat(movimiento(variante, "REINGRESO", "saldo_fisico")).isEqualTo(10);
        assertThat(movimiento(variante, "REINGRESO", "referencia")).isEqualTo("FS-000104");
    }

    @Test
    @DisplayName("CA-2-50 cancelar o expirar una reserva que ya no está activa no tiene efecto")
    void cancelarSobreReservaCerrada() {
        long variante = datos.variante("EVT04", 10, 0);
        UUID liberada = reservar(variante, 2);
        esperarProcesado(expirado(liberada, "FS-000105"));
        UUID reingresada = reservar(variante, 4);
        esperarProcesado(pagado(reingresada, "FS-000106", variante, 4));
        esperarProcesado(cancelado(reingresada, "FS-000106", true));
        assertThat(datos.fisico(variante)).isEqualTo(10);

        esperarProcesado(cancelado(liberada, "FS-000105", false));
        esperarProcesado(expirado(reingresada, "FS-000106"));
        esperarProcesado(cancelado(reingresada, "FS-000106", true));

        assertThat(datos.estadoReserva(liberada)).isEqualTo("LIBERADA");
        assertThat(datos.estadoReserva(reingresada)).isEqualTo("REINGRESADA");
        assertThat(datos.fisico(variante)).isEqualTo(10);
        assertThat(datos.reservado(variante)).isZero();
        assertThat(datos.movimientos(variante, "REINGRESO")).isEqualTo(1);
        assertThat(datos.estadoReserva(UUID.randomUUID())).isNull();
        esperarProcesado(cancelado(UUID.randomUUID(), "FS-999999", false));
    }

    // ------------------------------------------------------------ CA-2-51

    @Test
    @DisplayName("CA-2-51 el liberador libera la reserva vencida cuyo evento nunca llegó y queda EXPIRADA; deja intactas las demás")
    void liberadorDeVencidas() {
        long variante = datos.variante("EVT05", 10, 0);
        UUID huerfana = reservar(variante, 3);
        UUID vigente = reservar(variante, 2);
        UUID confirmada = reservar(variante, 1);
        jdbc.update("UPDATE reserva SET expira_en = now() - interval '1 minute' WHERE pedido_id = ?::uuid", huerfana.toString());
        jdbc.update("UPDATE reserva SET estado = 'CONFIRMADA', expira_en = now() - interval '1 hour' WHERE pedido_id = ?::uuid",
                confirmada.toString());

        int liberadas = liberador.liberarVencidas();

        assertThat(liberadas).isEqualTo(1);
        assertThat(datos.estadoReserva(huerfana)).isEqualTo("EXPIRADA");
        assertThat(datos.estadoReserva(vigente)).isEqualTo("ACTIVA");
        assertThat(datos.estadoReserva(confirmada)).isEqualTo("CONFIRMADA");
        assertThat(datos.reservado(variante)).isEqualTo(3); // quedan la vigente (2) y la confirmada (1)
        assertThat(liberador.liberarVencidas()).isZero();
    }

    // ------------------------------------------------------------ CA-2-52

    @Test
    @DisplayName("CA-2-52 PedidoPagado con la reserva vencida y sin stock suficiente no descuenta en negativo y publica StockNoConfirmado con los faltantes")
    void pagoSinStockTrasVencer() throws Exception {
        long variante = datos.variante("EVT06", 3, 0);
        UUID pedido = reservar(variante, 3);
        jdbc.update("UPDATE reserva SET expira_en = now() - interval '1 minute' WHERE pedido_id = ?::uuid", pedido.toString());
        liberador.liberarVencidas();
        assertThat(datos.estadoReserva(pedido)).isEqualTo("EXPIRADA");
        reservar(variante, 2); // otro cliente toma el stock liberado
        admin.purgeQueue(COLA_ALERTAS, true);

        esperarProcesado(pagado(pedido, "FS-000107", variante, 3));

        assertThat(datos.fisico(variante)).isEqualTo(3);
        assertThat(datos.reservado(variante)).isEqualTo(2);
        assertThat(datos.estadoReserva(pedido)).isEqualTo("EXPIRADA");
        assertThat(datos.movimientos(variante, "VENTA")).isZero();
        JsonNode alerta = esperarAlerta("StockNoConfirmado");
        assertThat(alerta.path("datos").path("pedidoId").asText()).isEqualTo(pedido.toString());
        JsonNode faltante = alerta.path("datos").path("faltantes").get(0);
        assertThat(faltante.path("varianteId").asLong()).isEqualTo(variante);
        assertThat(faltante.path("sku").asText()).isEqualTo("EVT06-NEG-M");
        assertThat(faltante.path("solicitado").asInt()).isEqualTo(3);
        assertThat(faltante.path("disponible").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-2-52 PedidoPagado con la reserva vencida pero con stock lo toma como una reserva nueva: CONFIRMADA y VENTA")
    void pagoTardioConStock() {
        long variante = datos.variante("EVT07", 5, 0);
        UUID pedido = reservar(variante, 3);
        jdbc.update("UPDATE reserva SET expira_en = now() - interval '1 minute' WHERE pedido_id = ?::uuid", pedido.toString());
        liberador.liberarVencidas();

        esperarProcesado(pagado(pedido, "FS-000108", variante, 3));

        assertThat(datos.fisico(variante)).isEqualTo(2);
        assertThat(datos.reservado(variante)).isZero();
        assertThat(datos.estadoReserva(pedido)).isEqualTo("CONFIRMADA");
        assertThat(movimiento(variante, "VENTA", "cantidad")).isEqualTo(-3);
        assertThat(datos.eventosPublicados("StockNoConfirmado")).isZero();
    }

    @Test
    @DisplayName("CA-2-52 PedidoPagado de un pedido sin reserva (o con la reserva liberada) se trata igual, con los ítems del evento")
    void pagoSinReserva() throws Exception {
        long variante = datos.variante("EVT08", 4, 0);
        long escasa = datos.variante("EVT09", 1, 0);
        UUID sinReserva = UUID.randomUUID();
        UUID sinStock = UUID.randomUUID();

        esperarProcesado(pagado(sinReserva, "FS-000109", variante, 2));
        esperarProcesado(pagado(sinStock, "FS-000110", escasa, 2));

        assertThat(datos.fisico(variante)).isEqualTo(2);
        assertThat(datos.estadoReserva(sinReserva)).isEqualTo("CONFIRMADA");
        assertThat(datos.movimientos(variante, "VENTA")).isEqualTo(1);
        assertThat(datos.fisico(escasa)).isEqualTo(1);
        assertThat(datos.estadoReserva(sinStock)).isNull();
        assertThat(esperarAlerta("StockNoConfirmado").path("datos").path("pedidoId").asText()).isEqualTo(sinStock.toString());
    }

    // ------------------------------------------------------------ CA-2-53

    @Test
    @DisplayName("CA-2-53 un evento repetido no tiene efecto la segunda vez")
    void eventosRepetidos() throws Exception {
        long variante = datos.variante("EVT10", 10, 0);
        UUID pedido = reservar(variante, 3);
        UUID eventoPago = UUID.randomUUID();

        pagado(pedido, eventoPago, variante, 3);
        esperarProcesado(eventoPago);
        // el mismo eventoId otra vez, y luego un evento distinto como señal de que ya se procesó todo lo anterior
        pagado(pedido, eventoPago, variante, 3);
        UUID senal = UUID.randomUUID();
        publicar(senal, "order.pedido.expirado", "PedidoExpirado", "{\"pedidoId\":\"" + UUID.randomUUID() + "\",\"codigo\":\"FS-1\"}");
        esperarProcesado(senal);

        assertThat(datos.fisico(variante)).isEqualTo(7);
        assertThat(datos.movimientos(variante, "VENTA")).isEqualTo(1);

        UUID eventoCancelacion = UUID.randomUUID();
        cancelado(pedido, eventoCancelacion, "FS-000111");
        esperarProcesado(eventoCancelacion);
        cancelado(pedido, eventoCancelacion, "FS-000111");
        UUID senal2 = UUID.randomUUID();
        publicar(senal2, "order.pedido.expirado", "PedidoExpirado", "{\"pedidoId\":\"" + UUID.randomUUID() + "\",\"codigo\":\"FS-2\"}");
        esperarProcesado(senal2);

        assertThat(datos.fisico(variante)).isEqualTo(10);
        assertThat(datos.movimientos(variante, "REINGRESO")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evento_procesado WHERE evento_id = ?::uuid", Integer.class,
                eventoPago.toString())).isOne();
    }

    private void pagado(UUID pedido, UUID eventoId, long variante, int cantidad) {
        publicar(eventoId, "order.pedido.pagado", "PedidoPagado", "{\"pedidoId\":\"" + pedido + "\",\"codigo\":\"FS-000111\","
                + "\"items\":[{\"varianteId\":" + variante + ",\"cantidad\":" + cantidad + "}]}");
    }

    private void cancelado(UUID pedido, UUID eventoId, String codigo) {
        publicar(eventoId, "order.pedido.cancelado", "PedidoCancelado", "{\"pedidoId\":\"" + pedido + "\",\"codigo\":\"" + codigo
                + "\",\"fuePagado\":true,\"motivo\":null,\"actor\":\"STAFF\",\"items\":[]}");
    }
}
