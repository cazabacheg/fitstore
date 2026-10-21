package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.PedidoRecogido;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** C11: el cliente recogió su pedido en la tienda. */
@Component
class ManejadorPedidoRecogido implements ManejadorEvento<PedidoRecogido> {

    private final CreadorEnvios envios;

    ManejadorPedidoRecogido(CreadorEnvios envios) {
        this.envios = envios;
    }

    @Override
    public String tipo() {
        return "PedidoRecogido";
    }

    @Override
    public Class<PedidoRecogido> clase() {
        return PedidoRecogido.class;
    }

    @Override
    public void manejar(UUID eventoId, PedidoRecogido evento) {
        envios.crear(eventoId, CatalogoPlantillas.ENTREGADO, evento.destinatario().email(),
                VariablesCorreo.nuevo().con("nombre", evento.destinatario().nombres())
                        .con("codigoPedido", evento.codigo()).construir(), evento.codigo(), null);
    }
}
