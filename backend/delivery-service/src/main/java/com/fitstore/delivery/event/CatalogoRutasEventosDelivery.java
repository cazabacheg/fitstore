package com.fitstore.delivery.event;

import com.fitstore.common.event.CatalogoRutasEventos;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Routing keys de los eventos que publica delivery, tomadas de los canales del AsyncAPI. */
@Component
public class CatalogoRutasEventosDelivery implements CatalogoRutasEventos {

    private static final Map<String, String> RUTAS = Map.of(
            "EntregaActualizada", "delivery.entrega.actualizada");

    @Override
    public String routingKey(String tipo) {
        String ruta = RUTAS.get(tipo);
        if (ruta == null) {
            throw new IllegalArgumentException("delivery no publica el evento " + tipo);
        }
        return ruta;
    }
}
