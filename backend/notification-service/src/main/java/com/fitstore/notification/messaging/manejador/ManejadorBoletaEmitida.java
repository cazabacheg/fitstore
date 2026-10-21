package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.ConfirmacionPedido;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.BoletaEmitida;
import com.fitstore.notification.repository.PedidoResumenRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * C5: "¡Pedido confirmado!" con la boleta adjunta (se descarga de payment al enviar). Si el respaldo de 10 minutos ya
 * confirmó el pedido sin adjunto, no se manda una segunda confirmación (CA-7-14).
 */
@Component
class ManejadorBoletaEmitida implements ManejadorEvento<BoletaEmitida> {

    private final PedidoResumenRepository pedidos;
    private final ConfirmacionPedido confirmacion;

    ManejadorBoletaEmitida(PedidoResumenRepository pedidos, ConfirmacionPedido confirmacion) {
        this.pedidos = pedidos;
        this.confirmacion = confirmacion;
    }

    @Override
    public String tipo() {
        return "BoletaEmitida";
    }

    @Override
    public Class<BoletaEmitida> clase() {
        return BoletaEmitida.class;
    }

    @Override
    public void manejar(UUID eventoId, BoletaEmitida evento) {
        // Sin la copia del pedido no hay a quién escribir: falla y el evento se reintenta (y va a la DLQ si no aparece)
        var pedido = pedidos.findById(evento.pedidoId()).orElseThrow(() -> new IllegalStateException(
                "BoletaEmitida de " + evento.pedidoId() + " sin copia del pedido (¿llegó PedidoCreado?)"));
        confirmacion.confirmar(eventoId, pedido, evento.numero());
    }
}
