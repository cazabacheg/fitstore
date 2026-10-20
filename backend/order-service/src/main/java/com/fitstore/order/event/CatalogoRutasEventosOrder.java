package com.fitstore.order.event;

import com.fitstore.common.event.CatalogoRutasEventos;
import org.springframework.stereotype.Component;

/** Routing keys de los 9 eventos que publica order: las direcciones de sus canales en el AsyncAPI. */
@Component
public class CatalogoRutasEventosOrder implements CatalogoRutasEventos {

    @Override
    public String routingKey(String tipo) {
        return switch (tipo) {
            case "PedidoCreado" -> "order.pedido.creado";
            case "PedidoPagado" -> "order.pedido.pagado";
            case "PedidoCancelado" -> "order.pedido.cancelado";
            case "PedidoExpirado" -> "order.pedido.expirado";
            case "PagoNoAplicable" -> "order.pago.no-aplicable";
            case "PedidoListoDespacho" -> "order.pedido.listo-despacho";
            case "PedidoListoRecojo" -> "order.pedido.listo-recojo";
            case "PedidoRecogido" -> "order.pedido.recogido";
            case "ReembolsoSolicitado" -> "order.reembolso.solicitado";
            default -> throw new IllegalArgumentException("Evento desconocido en order: " + tipo);
        };
    }
}
