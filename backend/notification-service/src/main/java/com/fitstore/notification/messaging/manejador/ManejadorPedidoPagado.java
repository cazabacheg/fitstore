package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.PedidoPagado;
import com.fitstore.notification.repository.PedidoResumenRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** No envía correo: marca cuándo se pagó el pedido para iniciar el respaldo de 10 minutos de la boleta (4.3). */
@Component
class ManejadorPedidoPagado implements ManejadorEvento<PedidoPagado> {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorPedidoPagado.class);

    private final PedidoResumenRepository pedidos;
    private final Clock reloj;

    ManejadorPedidoPagado(PedidoResumenRepository pedidos, Clock reloj) {
        this.pedidos = pedidos;
        this.reloj = reloj;
    }

    @Override
    public String tipo() {
        return "PedidoPagado";
    }

    @Override
    public Class<PedidoPagado> clase() {
        return PedidoPagado.class;
    }

    @Override
    public void manejar(UUID eventoId, PedidoPagado evento) {
        pedidos.findById(evento.pedidoId()).ifPresentOrElse(resumen -> {
            if (resumen.getPagadoEn() == null) {
                resumen.setPagadoEn(reloj.instant());
                pedidos.save(resumen);
            }
        }, () -> LOG.warn("PedidoPagado de {} sin copia del pedido: no se puede confirmar por correo", evento.codigo()));
    }
}
