package com.fitstore.order.pedidos.messaging;

import com.fitstore.common.event.ConsumidorIdempotente;
import com.fitstore.common.event.SobreEvento;
import com.fitstore.order.event.BoletaEmitida;
import com.fitstore.order.event.EntregaActualizada;
import com.fitstore.order.event.PagoConfirmado;
import com.fitstore.order.event.PagoRechazado;
import com.fitstore.order.pedidos.service.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumidor de los eventos que mueven el pedido (micro 6): {@code PagoConfirmado}, {@code PagoRechazado},
 * {@code BoletaEmitida} (payment) y {@code EntregaActualizada} (delivery). Es idempotente por {@code eventoId}
 * (tabla {@code evento_procesado}): un evento repetido no tiene efecto. Si el manejador falla,
 * {@link ConsumidorIdempotente} reintenta y luego manda el mensaje a la DLQ.
 */
@Component
public class ConsumidorEventos {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final ConsumidorIdempotente idempotente;
    private final PedidoService pedidos;
    private final JsonMapper json;

    public ConsumidorEventos(ConsumidorIdempotente idempotente, PedidoService pedidos, JsonMapper json) {
        this.idempotente = idempotente;
        this.pedidos = pedidos;
        this.json = json;
    }

    @RabbitListener(queues = "${fitstore.eventos.servicio}.eventos", autoStartup = "${fitstore.eventos.escuchar:true}")
    public void alRecibir(SobreEvento sobre) {
        idempotente.procesar(sobre, this::despachar);
    }

    private void despachar(SobreEvento sobre) {
        switch (sobre.tipo()) {
            case "PagoConfirmado" -> pedidos.aplicarPagoConfirmado(json.treeToValue(sobre.datos(), PagoConfirmado.class));
            case "PagoRechazado" -> pedidos.aplicarPagoRechazado(json.treeToValue(sobre.datos(), PagoRechazado.class));
            case "BoletaEmitida" -> pedidos.aplicarBoleta(json.treeToValue(sobre.datos(), BoletaEmitida.class));
            case "EntregaActualizada" ->
                    pedidos.aplicarEntrega(json.treeToValue(sobre.datos(), EntregaActualizada.class));
            default -> LOG.debug("Evento {} sin manejador en order; se ignora", sobre.tipo());
        }
    }
}
