package com.fitstore.delivery.messaging;

import com.fitstore.common.event.ConsumidorIdempotente;
import com.fitstore.common.event.SobreEvento;
import com.fitstore.delivery.event.PedidoListoDespacho;
import com.fitstore.delivery.service.EntregaService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consume de la cola {@code delivery.eventos} el evento {@code PedidoListoDespacho} y crea la entrega (RF-DEL-01).
 * Es idempotente de dos maneras: por {@code eventoId} (tabla {@code evento_procesado}) y por {@code pedidoId} único.
 */
@Component
public class ConsumidorPedidos {

    static final String TIPO_PEDIDO_LISTO = "PedidoListoDespacho";

    private final ConsumidorIdempotente idempotente;
    private final EntregaService entregas;
    private final JsonMapper json;

    public ConsumidorPedidos(ConsumidorIdempotente idempotente, EntregaService entregas, JsonMapper json) {
        this.idempotente = idempotente;
        this.entregas = entregas;
        this.json = json;
    }

    @RabbitListener(queues = "${fitstore.eventos.servicio}.eventos")
    public void alRecibir(SobreEvento sobre) {
        if (!TIPO_PEDIDO_LISTO.equals(sobre.tipo())) {
            return;
        }
        idempotente.procesar(sobre, s -> entregas.crearDesde(json.treeToValue(s.datos(), PedidoListoDespacho.class)));
    }
}
