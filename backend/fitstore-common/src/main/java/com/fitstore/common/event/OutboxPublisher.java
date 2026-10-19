package com.fitstore.common.event;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Publica las filas pendientes de {@code outbox_evento} (ADR-009, infraestructura 8): mensaje persistente con
 * confirmación del publicador; la fila se marca {@code publicado_en} <strong>solo</strong> cuando RabbitMQ confirma.
 * Si RabbitMQ no está disponible o no confirma, la fila queda pendiente y se reintenta en la siguiente ejecución.
 */
public class OutboxPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventoRepository repositorio;
    private final RabbitTemplate rabbitTemplate;
    private final CatalogoRutasEventos rutas;
    private final TransactionTemplate transaccion;
    private final JsonMapper json;
    private final String exchange;
    private final OutboxProperties propiedades;

    public OutboxPublisher(OutboxEventoRepository repositorio, RabbitTemplate rabbitTemplate,
            CatalogoRutasEventos rutas, TransactionTemplate transaccion, JsonMapper json,
            EventosProperties eventos, OutboxProperties propiedades) {
        this.repositorio = repositorio;
        this.rabbitTemplate = rabbitTemplate;
        this.rutas = rutas;
        this.transaccion = transaccion;
        this.json = json;
        this.exchange = eventos.exchange();
        this.propiedades = propiedades;
    }

    @Scheduled(fixedDelayString = "${fitstore.outbox.intervalo-ms:1000}",
            initialDelayString = "${fitstore.outbox.intervalo-ms:1000}")
    public void publicarProgramado() {
        publicarPendientes();
    }

    /** @return cuántas filas se publicaron y confirmaron en esta ejecución */
    public int publicarPendientes() {
        List<OutboxEvento> pendientes = repositorio.findByPublicadoEnIsNullOrderByCreadoEnAsc(
                PageRequest.of(0, propiedades.lote()));
        int publicados = 0;
        for (OutboxEvento evento : pendientes) {
            String routingKey;
            try {
                routingKey = rutas.routingKey(evento.getTipo());
            } catch (RuntimeException e) {
                LOG.error("Evento {} ({}) sin routing key; queda pendiente: {}", evento.getId(), evento.getTipo(), e.getMessage());
                continue;
            }
            try {
                if (!enviarYEsperarConfirmacion(evento, routingKey)) {
                    LOG.warn("RabbitMQ no confirmó el evento {} ({}); queda pendiente", evento.getId(), evento.getTipo());
                    break;
                }
            } catch (AmqpException e) {
                LOG.warn("RabbitMQ no está disponible; el evento {} queda pendiente: {}", evento.getId(), e.getMessage());
                break;
            }
            transaccion.executeWithoutResult(estado -> repositorio.marcarPublicado(evento.getId(), Instant.now()));
            publicados++;
        }
        return publicados;
    }

    private boolean enviarYEsperarConfirmacion(OutboxEvento evento, String routingKey) {
        Message mensaje = MessageBuilder.withBody(construirSobre(evento).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .setContentType("application/json")
                .setContentEncoding("UTF-8")
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(evento.getId().toString())
                .setType(evento.getTipo())
                .build();
        CorrelationData correlacion = new CorrelationData(evento.getId().toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, mensaje, correlacion);
        try {
            return correlacion.getFuture().get(propiedades.esperaConfirmacionMs(), TimeUnit.MILLISECONDS).isAck();
        } catch (TimeoutException | ExecutionException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String construirSobre(OutboxEvento evento) {
        ObjectNode sobre = json.createObjectNode();
        sobre.put("eventoId", evento.getId().toString());
        sobre.put("tipo", evento.getTipo());
        sobre.put("version", SobreEvento.VERSION_ACTUAL);
        sobre.put("ocurridoEn", evento.getCreadoEn().toString());
        sobre.set("datos", json.readTree(evento.getPayload()));
        return json.writeValueAsString(sobre);
    }
}
