package com.fitstore.delivery.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fitstore.delivery.pruebas.PostgresPrueba;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

/** Integración con RabbitMQ real: PedidoListoDespacho crea una entrega, y un duplicado no crea otra (CA-6-19). */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=true")
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
class ConsumidorPedidosIT {

    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.3.5-management");

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void rabbit(DynamicPropertyRegistry registro) {
        registro.add("spring.rabbitmq.host", RABBIT::getHost);
        registro.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registro.add("spring.rabbitmq.username", () -> "guest");
        registro.add("spring.rabbitmq.password", () -> "guest");
    }

    @Autowired RabbitTemplate rabbit;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM entrega_evento");
        jdbc.update("DELETE FROM entrega");
        jdbc.update("DELETE FROM outbox_evento");
        jdbc.update("DELETE FROM evento_procesado");
    }

    private Map<String, Object> sobre(UUID eventoId, UUID pedidoId) {
        return Map.of("eventoId", eventoId.toString(), "tipo", "PedidoListoDespacho", "version", 1,
                "ocurridoEn", Instant.now().toString(),
                "datos", Map.of("pedidoId", pedidoId.toString(), "codigo", "FS-000501", "modalidad", "DELIVERY_LOCAL",
                        "destinatario", Map.of("nombres", "Lucía", "apellidos", "Mendoza", "dni", "45678912",
                                "telefono", "955111222", "email", "lucia@correo.pe"),
                        "direccion", Map.of("ubigeo", "150122", "departamento", "Lima", "provincia", "Lima",
                                "distrito", "Miraflores", "direccion", "Av. Larco 345"),
                        "resumenItems", "Legging Pro M × 2", "bultos", 1));
    }

    private int entregasDelPedido(UUID pedidoId) {
        return jdbc.queryForObject("SELECT count(*) FROM entrega WHERE pedido_id = ?", Integer.class, pedidoId);
    }

    @Test
    @DisplayName("CA-6-19 PedidoListoDespacho crea la entrega y, repetido (mismo evento o mismo pedido), sigue habiendo una sola")
    void creaUnaSolaEntrega() {
        UUID pedidoId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();

        rabbit.convertAndSend("fitstore.eventos", "order.pedido.listo-despacho", sobre(eventoId, pedidoId));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(entregasDelPedido(pedidoId)).isEqualTo(1));

        // El mismo evento otra vez, y otro evento distinto para el mismo pedido
        rabbit.convertAndSend("fitstore.eventos", "order.pedido.listo-despacho", sobre(eventoId, pedidoId));
        rabbit.convertAndSend("fitstore.eventos", "order.pedido.listo-despacho", sobre(UUID.randomUUID(), pedidoId));
        // Un evento de otro tipo que llegara a la cola se ignora
        Map<String, Object> ajeno = new java.util.HashMap<>(sobre(UUID.randomUUID(), UUID.randomUUID()));
        ajeno.put("tipo", "PedidoCreado");
        rabbit.convertAndSend("fitstore.eventos", "order.pedido.listo-despacho", ajeno);
        UUID otroPedido = UUID.randomUUID();
        rabbit.convertAndSend("fitstore.eventos", "order.pedido.listo-despacho", sobre(UUID.randomUUID(), otroPedido));
        // Cuando el pedido que llegó al final ya está, los duplicados anteriores ya se procesaron (misma cola, en orden)
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(entregasDelPedido(otroPedido)).isEqualTo(1));

        assertThat(entregasDelPedido(pedidoId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM entrega", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega WHERE pedido_id = ?", String.class, pedidoId))
                .isEqualTo("PENDIENTE_ASIGNACION");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE tipo = 'EntregaActualizada'", Integer.class))
                .isEqualTo(2);
    }
}
