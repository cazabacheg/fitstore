package com.fitstore.catalog.inventario.messaging;

import com.fitstore.catalog.event.PedidoCancelado;
import com.fitstore.catalog.event.PedidoExpirado;
import com.fitstore.catalog.event.PedidoPagado;
import com.fitstore.catalog.inventario.service.ReservaService;
import com.fitstore.common.event.ConsumidorIdempotente;
import com.fitstore.common.event.SobreEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumidor de los eventos de order que afectan al stock (micro 6): {@code PedidoPagado}, {@code PedidoCancelado} y
 * {@code PedidoExpirado}. Es idempotente por {@code eventoId} (tabla {@code evento_procesado}): un evento repetido no tiene
 * efecto. Si el manejador falla, {@link ConsumidorIdempotente} reintenta y luego manda el mensaje a la DLQ.
 */
@Component
public class ConsumidorPedidos {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorPedidos.class);

    private final ConsumidorIdempotente idempotente;
    private final ReservaService reservas;
    private final JsonMapper json;

    public ConsumidorPedidos(ConsumidorIdempotente idempotente, ReservaService reservas, JsonMapper json) {
        this.idempotente = idempotente;
        this.reservas = reservas;
        this.json = json;
    }

    @RabbitListener(queues = "${fitstore.eventos.servicio}.eventos", autoStartup = "${fitstore.eventos.escuchar:true}")
    public void alRecibir(SobreEvento sobre) {
        idempotente.procesar(sobre, this::despachar);
    }

    private void despachar(SobreEvento sobre) {
        switch (sobre.tipo()) {
            case "PedidoPagado" -> {
                PedidoPagado e = json.treeToValue(sobre.datos(), PedidoPagado.class);
                reservas.confirmarPago(e.pedidoId(), e.codigo(), e.items());
            }
            case "PedidoCancelado" -> {
                PedidoCancelado e = json.treeToValue(sobre.datos(), PedidoCancelado.class);
                reservas.cancelar(e.pedidoId(), e.codigo());
            }
            case "PedidoExpirado" -> {
                PedidoExpirado e = json.treeToValue(sobre.datos(), PedidoExpirado.class);
                reservas.expirar(e.pedidoId());
            }
            default -> LOG.debug("Evento {} sin manejador en catalog; se ignora", sobre.tipo());
        }
    }
}
