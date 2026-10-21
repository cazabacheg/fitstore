package com.fitstore.payment.messaging;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fitstore.payment.pruebas.PostgresPrueba;
import com.fitstore.payment.pruebas.RabbitPrueba;
import java.time.Instant;
import java.util.UUID;
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

/** Intención de pago por pedido a partir de los eventos de order, con RabbitMQ real (HU-5-01: CA-5-01 y CA-5-02). */
@SpringBootTest
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
class IntencionPagoConsumidorIT {

    @DynamicPropertySource
    static void rabbit(DynamicPropertyRegistry registro) {
        RabbitPrueba.registrar(registro);
    }

    @Autowired RabbitTemplate rabbit;
    @Autowired JdbcTemplate jdbc;

    private static String pedidoCreado(UUID pedidoId, String codigo) {
        return """
                {"pedidoId":"%s","codigo":"%s","clienteId":"%s",
                 "destinatario":{"nombres":"Ana","apellidos":"Quispe","dni":"12345678","telefono":"987654321","email":"ana@correo.pe"},
                 "modalidad":"RECOJO","direccion":null,
                 "items":[{"varianteId":1,"productoId":1,"sku":"POL-001-M-NEG","producto":"Polo Dry-Fit","talla":"M","color":"Negro",
                           "imagenUrl":null,"precioUnitario":59.90,"cantidad":2,"descuentoPorcentaje":0,"descuentoMonto":0,"total":119.80}],
                 "subtotal":119.80,"descuentoTotal":0,"costoEnvio":10.00,"total":129.80,"expiraEn":"%s"}
                """.formatted(pedidoId, codigo, UUID.randomUUID(), Instant.now().plusSeconds(1800));
    }

    private void esperarProcesado(UUID eventoId) {
        await().atMost(15, SECONDS).pollInterval(100, java.util.concurrent.TimeUnit.MILLISECONDS).until(() ->
                jdbc.queryForObject("SELECT count(*) FROM evento_procesado WHERE evento_id = ?::uuid", Integer.class,
                        eventoId.toString()) == 1);
    }

    private UUID crear(UUID pedidoId, String codigo) {
        UUID evento = RabbitPrueba.publicar(rabbit, UUID.randomUUID(), "order.pedido.creado", "PedidoCreado",
                pedidoCreado(pedidoId, codigo));
        esperarProcesado(evento);
        return evento;
    }

    private String estado(UUID pedidoId) {
        return jdbc.queryForObject("SELECT estado FROM intencion_pago WHERE pedido_id = ?::uuid", String.class,
                pedidoId.toString());
    }

    @Test
    @DisplayName("CA-5-01 PedidoCreado crea una intención PENDIENTE con monto, ítems, cliente y vencimiento")
    void creaLaIntencion() {
        UUID pedido = UUID.randomUUID();
        crear(pedido, "FS-100001");

        var fila = jdbc.queryForMap("SELECT * FROM intencion_pago WHERE pedido_id = ?::uuid", pedido.toString());
        assertThat(fila.get("estado")).isEqualTo("PENDIENTE");
        assertThat(fila.get("codigo_pedido")).isEqualTo("FS-100001");
        assertThat((java.math.BigDecimal) fila.get("monto")).isEqualByComparingTo("129.80");
        assertThat((java.math.BigDecimal) fila.get("costo_envio")).isEqualByComparingTo("10.00");
        assertThat(fila.get("items").toString()).contains("Polo Dry-Fit").contains("Negro");
        assertThat(fila.get("cliente").toString()).contains("Ana Quispe").contains("12345678").contains("ana@correo.pe");
        assertThat(fila.get("expira_en")).isNotNull();
    }

    @Test
    @DisplayName("CA-5-01 el mismo PedidoCreado dos veces (aun con otro eventoId) deja una sola intención")
    void eventoDuplicado() {
        UUID pedido = UUID.randomUUID();
        String datos = pedidoCreado(pedido, "FS-100002");
        UUID primero = RabbitPrueba.publicar(rabbit, UUID.randomUUID(), "order.pedido.creado", "PedidoCreado", datos);
        UUID repetidoMismoId = primero;
        UUID otroId = RabbitPrueba.publicar(rabbit, UUID.randomUUID(), "order.pedido.creado", "PedidoCreado", datos);
        RabbitPrueba.publicar(rabbit, repetidoMismoId, "order.pedido.creado", "PedidoCreado", datos);
        esperarProcesado(primero);
        esperarProcesado(otroId);

        Integer filas = jdbc.queryForObject("SELECT count(*) FROM intencion_pago WHERE pedido_id = ?::uuid",
                Integer.class, pedido.toString());
        assertThat(filas).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-02 PedidoExpirado anula la intención")
    void expiradoAnula() {
        UUID pedido = UUID.randomUUID();
        crear(pedido, "FS-100003");

        UUID evento = RabbitPrueba.publicar(rabbit, UUID.randomUUID(), "order.pedido.expirado", "PedidoExpirado",
                "{\"pedidoId\":\"" + pedido + "\",\"codigo\":\"FS-100003\"}");
        esperarProcesado(evento);

        assertThat(estado(pedido)).isEqualTo("ANULADA");
    }

    @Test
    @DisplayName("CA-5-02 PedidoCancelado sin pago anula la intención")
    void canceladoSinPagoAnula() {
        UUID pedido = UUID.randomUUID();
        crear(pedido, "FS-100004");

        UUID evento = RabbitPrueba.publicar(rabbit, UUID.randomUUID(), "order.pedido.cancelado", "PedidoCancelado",
                "{\"pedidoId\":\"" + pedido + "\",\"codigo\":\"FS-100004\",\"fuePagado\":false,\"motivo\":null,"
                        + "\"actor\":\"CLIENTE\",\"items\":[]}");
        esperarProcesado(evento);

        assertThat(estado(pedido)).isEqualTo("ANULADA");
    }

    @Test
    @DisplayName("CA-5-02 PedidoCancelado de un pedido pagado no anula la intención (el reembolso es extensión de alcance)")
    void canceladoPagadoNoAnula() {
        UUID pedido = UUID.randomUUID();
        crear(pedido, "FS-100005");
        jdbc.update("UPDATE intencion_pago SET estado = 'PAGADA' WHERE pedido_id = ?::uuid", pedido.toString());

        UUID evento = RabbitPrueba.publicar(rabbit, UUID.randomUUID(), "order.pedido.cancelado", "PedidoCancelado",
                "{\"pedidoId\":\"" + pedido + "\",\"codigo\":\"FS-100005\",\"fuePagado\":true,\"motivo\":null,"
                        + "\"actor\":\"ADMIN\",\"items\":[]}");
        esperarProcesado(evento);

        assertThat(estado(pedido)).isEqualTo("PAGADA");
    }
}
