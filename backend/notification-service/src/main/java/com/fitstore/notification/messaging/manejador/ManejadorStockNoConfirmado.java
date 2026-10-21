package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.Faltante;
import com.fitstore.notification.messaging.evento.StockNoConfirmado;
import com.fitstore.notification.model.PedidoResumen;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.VariablesCorreo;
import com.fitstore.notification.repository.PedidoResumenRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** T2: alerta a la tienda de que un pedido pagado no encontró stock; el código sale de la copia del pedido. */
@Component
class ManejadorStockNoConfirmado implements ManejadorEvento<StockNoConfirmado> {

    private final CreadorEnvios envios;
    private final PedidoResumenRepository pedidos;

    ManejadorStockNoConfirmado(CreadorEnvios envios, PedidoResumenRepository pedidos) {
        this.envios = envios;
        this.pedidos = pedidos;
    }

    @Override
    public String tipo() {
        return "StockNoConfirmado";
    }

    @Override
    public Class<StockNoConfirmado> clase() {
        return StockNoConfirmado.class;
    }

    @Override
    public void manejar(UUID eventoId, StockNoConfirmado evento) {
        String codigo = pedidos.findById(evento.pedidoId()).map(PedidoResumen::getCodigo)
                .orElse(evento.pedidoId().toString());
        VariablesCorreo variables = VariablesCorreo.nuevo().con("codigoPedido", codigo);
        for (Faltante faltante : evento.faltantes()) {
            variables.fila(faltante.sku(), "Pidieron " + faltante.solicitado() + " y hay " + faltante.disponible());
        }
        envios.crearAlertas(eventoId, CatalogoPlantillas.ALERTA_SIN_STOCK, variables.construir(), codigo);
    }
}
