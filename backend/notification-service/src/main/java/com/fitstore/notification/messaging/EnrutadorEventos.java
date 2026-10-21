package com.fitstore.notification.messaging;

import com.fitstore.common.event.ConsumidorIdempotente;
import com.fitstore.common.event.SobreEvento;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Recibe todos los eventos de la cola {@code notification.eventos} y los entrega al manejador de su tipo (micro 4.2,
 * SPEC-7 N2). Recibir es rápido y no envía nada: el SMTP lo atiende después el despachador, así que un correo caído no
 * acumula la cola. Un evento ya procesado se ignora y los que fallan repetidamente van a la DLQ.
 */
@Component
public class EnrutadorEventos {

    private static final Logger LOG = LoggerFactory.getLogger(EnrutadorEventos.class);

    private final ConsumidorIdempotente consumidor;
    private final JsonMapper json;
    private final Map<String, ManejadorEvento<?>> manejadores = new HashMap<>();

    public EnrutadorEventos(ConsumidorIdempotente consumidor, JsonMapper json, List<ManejadorEvento<?>> manejadores) {
        this.consumidor = consumidor;
        this.json = json;
        manejadores.forEach(m -> this.manejadores.put(m.tipo(), m));
    }

    @RabbitListener(queues = "${fitstore.eventos.servicio}.eventos")
    public void alRecibir(SobreEvento sobre) {
        procesar(sobre);
    }

    /** Procesa un evento una sola vez, en una transacción junto con el registro de que ya se procesó. */
    public void procesar(SobreEvento sobre) {
        consumidor.procesar(sobre, this::enrutar);
    }

    private void enrutar(SobreEvento sobre) {
        ManejadorEvento<?> manejador = manejadores.get(sobre.tipo());
        if (manejador == null) {
            LOG.warn("Evento {} de tipo {} sin manejador; se ignora", sobre.eventoId(), sobre.tipo());
            return;
        }
        entregar(manejador, sobre);
    }

    private <T> void entregar(ManejadorEvento<T> manejador, SobreEvento sobre) {
        manejador.manejar(sobre.eventoId(), json.treeToValue(sobre.datos(), manejador.clase()));
    }
}
