package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.envio.CreadorEnvios;
import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.StockBajo;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.plantillas.VariablesCorreo;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** T1: alerta a la tienda de que una variante bajó de su stock mínimo. */
@Component
class ManejadorStockBajo implements ManejadorEvento<StockBajo> {

    private final CreadorEnvios envios;

    ManejadorStockBajo(CreadorEnvios envios) {
        this.envios = envios;
    }

    @Override
    public String tipo() {
        return "StockBajo";
    }

    @Override
    public Class<StockBajo> clase() {
        return StockBajo.class;
    }

    @Override
    public void manejar(UUID eventoId, StockBajo evento) {
        VariablesCorreo variables = VariablesCorreo.nuevo()
                .con("sku", evento.sku())
                .con("producto", evento.producto())
                .con("talla", evento.talla())
                .con("color", evento.color())
                .con("disponible", String.valueOf(evento.disponible()))
                .con("minimo", String.valueOf(evento.minimo()));
        envios.crearAlertas(eventoId, CatalogoPlantillas.ALERTA_STOCK_BAJO, variables.construir(), null);
    }
}
