package com.fitstore.catalog.event;

import com.fitstore.common.event.CatalogoRutasEventos;
import org.springframework.stereotype.Component;

/** Routing keys de los eventos que publica catalog: las direcciones de sus canales en el AsyncAPI. */
@Component
public class CatalogoRutasEventosCatalog implements CatalogoRutasEventos {

    @Override
    public String routingKey(String tipo) {
        return switch (tipo) {
            case "StockBajo" -> "catalog.stock.bajo";
            case "StockNoConfirmado" -> "catalog.stock.no-confirmado";
            default -> throw new IllegalArgumentException("Evento desconocido en catalog: " + tipo);
        };
    }
}
