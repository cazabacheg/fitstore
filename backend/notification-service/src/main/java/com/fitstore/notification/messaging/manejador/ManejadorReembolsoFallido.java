package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.ReembolsoFallido;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.FormatoEsPe;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** T3: alerta a la tienda de que un reembolso no se pudo hacer. */
@Component
class ManejadorReembolsoFallido implements ManejadorEvento<ReembolsoFallido> {

    private final CreadorEnvios envios;

    ManejadorReembolsoFallido(CreadorEnvios envios) {
        this.envios = envios;
    }

    @Override
    public String tipo() {
        return "ReembolsoFallido";
    }

    @Override
    public Class<ReembolsoFallido> clase() {
        return ReembolsoFallido.class;
    }

    @Override
    public void manejar(UUID eventoId, ReembolsoFallido evento) {
        VariablesCorreo variables = VariablesCorreo.nuevo()
                .con("codigoPedido", evento.codigoPedido())
                .con("monto", FormatoEsPe.moneda(evento.monto()))
                .con("ultimoError", evento.ultimoError())
                .fila("Intentos", String.valueOf(evento.intentos()));
        envios.crearAlertas(eventoId, CatalogoPlantillas.ALERTA_REEMBOLSO_FALLIDO, variables.construir(),
                evento.codigoPedido());
    }
}
