package com.fitstore.payment.messaging;

import com.fitstore.common.event.ConsumidorIdempotente;
import com.fitstore.common.event.SobreEvento;
import com.fitstore.payment.event.PedidoCancelado;
import com.fitstore.payment.event.PedidoCreado;
import com.fitstore.payment.event.PedidoExpirado;
import com.fitstore.payment.service.IntencionPagoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumidor de los eventos de order que afectan a la intención de pago: {@code PedidoCreado}, {@code PedidoExpirado} y
 * {@code PedidoCancelado}. Es idempotente por {@code eventoId}.
 *
 * <p>Los eventos de reembolso ({@code PagoNoAplicable}, {@code ReembolsoSolicitado}) y la devolución de un pedido
 * cancelado después de pagar son extensión de alcance (docs/implementacion/extension-alcance.md): se ignoran.
 */
@Component
public class ConsumidorEventos {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final ConsumidorIdempotente idempotente;
    private final IntencionPagoService intenciones;
    private final JsonMapper json;

    public ConsumidorEventos(ConsumidorIdempotente idempotente, IntencionPagoService intenciones, JsonMapper json) {
        this.idempotente = idempotente;
        this.intenciones = intenciones;
        this.json = json;
    }

    @RabbitListener(queues = "${fitstore.eventos.servicio}.eventos", autoStartup = "${fitstore.eventos.escuchar:true}")
    public void alRecibir(SobreEvento sobre) {
        idempotente.procesar(sobre, this::despachar);
    }

    private void despachar(SobreEvento sobre) {
        switch (sobre.tipo()) {
            case "PedidoCreado" -> intenciones.crear(json.treeToValue(sobre.datos(), PedidoCreado.class));
            case "PedidoExpirado" -> intenciones.anular(json.treeToValue(sobre.datos(), PedidoExpirado.class).pedidoId());
            case "PedidoCancelado" -> {
                PedidoCancelado cancelado = json.treeToValue(sobre.datos(), PedidoCancelado.class);
                if (cancelado.fuePagado()) {
                    LOG.info("Pedido {} cancelado después de pagar: la devolución se hace fuera del sistema", cancelado.codigo());
                } else {
                    intenciones.anular(cancelado.pedidoId());
                }
            }
            default -> LOG.debug("Evento {} sin manejador en payment; se ignora", sobre.tipo());
        }
    }
}
