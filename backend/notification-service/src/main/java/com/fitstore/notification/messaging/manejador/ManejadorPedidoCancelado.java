package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.envio.Enlaces;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.PedidoCancelado;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.VariablesCorreo;
import com.fitstore.notification.repository.PedidoResumenRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** C6: el pedido se canceló; si ya estaba pagado, avisa que se procesará el reembolso. */
@Component
class ManejadorPedidoCancelado implements ManejadorEvento<PedidoCancelado> {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorPedidoCancelado.class);

    private final CreadorEnvios envios;
    private final PedidoResumenRepository pedidos;
    private final Enlaces enlaces;

    ManejadorPedidoCancelado(CreadorEnvios envios, PedidoResumenRepository pedidos, Enlaces enlaces) {
        this.envios = envios;
        this.pedidos = pedidos;
        this.enlaces = enlaces;
    }

    @Override
    public String tipo() {
        return "PedidoCancelado";
    }

    @Override
    public Class<PedidoCancelado> clase() {
        return PedidoCancelado.class;
    }

    @Override
    public void manejar(UUID eventoId, PedidoCancelado evento) {
        pedidos.findById(evento.pedidoId()).ifPresentOrElse(pedido -> {
            VariablesCorreo variables = VariablesCorreo.nuevo()
                    .con("nombre", pedido.getNombre())
                    .con("codigoPedido", evento.codigo())
                    .con("motivo", evento.motivo())
                    .fila("Motivo", evento.motivo())
                    .boton(enlaces.pedido(evento.codigo()));
            if (evento.fuePagado()) {
                variables.fila("Reembolso", "Procesaremos la devolución de tu dinero y te avisaremos.");
            }
            envios.crear(eventoId, CatalogoPlantillas.PEDIDO_CANCELADO, pedido.getEmail(), variables.construir(),
                    evento.codigo(), null);
        }, () -> LOG.warn("PedidoCancelado de {} sin copia del pedido: no hay a quién escribir", evento.codigo()));
    }
}
