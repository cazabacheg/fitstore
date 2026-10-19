package com.fitstore.common.event;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consumidor idempotente (ADR-009, infraestructura 8): ignora un {@code eventoId} ya procesado (tabla
 * {@code evento_procesado}), ejecuta el manejador y registra el evento en la <strong>misma transacción</strong>.
 * Si el manejador falla, reintenta con espera creciente; tras agotar los intentos rechaza el mensaje sin
 * reencolar, y RabbitMQ lo manda a la cola {@code {servicio}.eventos.dlq}.
 */
public class ConsumidorIdempotente {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorIdempotente.class);

    private final EventoProcesadoRepository repositorio;
    private final TransactionTemplate transaccion;
    private final EventosProperties propiedades;

    public ConsumidorIdempotente(EventoProcesadoRepository repositorio, TransactionTemplate transaccion,
            EventosProperties propiedades) {
        this.repositorio = repositorio;
        this.transaccion = transaccion;
        this.propiedades = propiedades;
    }

    public void procesar(SobreEvento sobre, Consumer<SobreEvento> manejador) {
        UUID eventoId = sobre.eventoId();
        long espera = propiedades.esperaInicialMs();
        RuntimeException ultimoFallo = null;
        for (int intento = 1; intento <= propiedades.reintentos(); intento++) {
            try {
                Boolean ejecutado = transaccion.execute(estado -> {
                    if (repositorio.existsById(eventoId)) {
                        return false;
                    }
                    manejador.accept(sobre);
                    repositorio.saveAndFlush(new EventoProcesado(eventoId, Instant.now()));
                    return true;
                });
                if (!Boolean.TRUE.equals(ejecutado)) {
                    LOG.info("Evento {} ({}) ya procesado; se ignora", eventoId, sobre.tipo());
                }
                return;
            } catch (RuntimeException e) {
                ultimoFallo = e;
                LOG.warn("Falló el intento {}/{} del evento {} ({}): {}", intento, propiedades.reintentos(),
                        eventoId, sobre.tipo(), e.getMessage());
                if (intento < propiedades.reintentos() && !esperar(espera)) {
                    break;
                }
                espera = Math.round(espera * propiedades.multiplicador());
            }
        }
        throw new AmqpRejectAndDontRequeueException(
                "El evento " + eventoId + " (" + sobre.tipo() + ") agotó sus intentos; va a la DLQ", ultimoFallo);
    }

    private static boolean esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
