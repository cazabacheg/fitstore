package com.fitstore.payment.pruebas;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/** RabbitMQ en Testcontainers (la misma etiqueta del compose), compartido por las pruebas de eventos de payment. */
public final class RabbitPrueba {

    public static final String EXCHANGE = "fitstore.eventos";

    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.3.5-management");

    static {
        RABBIT.start();
    }

    private RabbitPrueba() {
    }

    /** Apunta el servicio al RabbitMQ de la prueba, enciende los consumidores y acelera el publicador del outbox. */
    public static void registrar(DynamicPropertyRegistry registro) {
        registro.add("spring.rabbitmq.host", RABBIT::getHost);
        registro.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registro.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registro.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registro.add("fitstore.eventos.escuchar", () -> "true");
        registro.add("fitstore.outbox.intervalo-ms", () -> "200");
    }

    /** Publica un evento con el sobre común (K12) como lo haría order. */
    public static UUID publicar(RabbitTemplate rabbit, UUID eventoId, String routingKey, String tipo, String datosJson) {
        String sobre = "{\"eventoId\":\"" + eventoId + "\",\"tipo\":\"" + tipo + "\",\"version\":1,\"ocurridoEn\":\""
                + Instant.now() + "\",\"datos\":" + datosJson + "}";
        Message mensaje = MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8))
                .setContentType("application/json").build();
        rabbit.send(EXCHANGE, routingKey, mensaje);
        return eventoId;
    }
}
