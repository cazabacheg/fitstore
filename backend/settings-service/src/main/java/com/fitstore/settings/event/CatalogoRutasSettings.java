package com.fitstore.settings.event;

import com.fitstore.common.event.CatalogoRutasEventos;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Routing keys de los eventos que publica settings, según los canales de {@code fitstore-eventos.yaml}. */
@Component
public class CatalogoRutasSettings implements CatalogoRutasEventos {

    private static final Map<String, String> RUTAS = Map.of(
            "ConfiguracionActualizada", "settings.configuracion.actualizada",
            "ReclamoRegistrado", "settings.reclamo.registrado",
            "ReclamoRespondido", "settings.reclamo.respondido");

    @Override
    public String routingKey(String tipo) {
        String ruta = RUTAS.get(tipo);
        if (ruta == null) {
            throw new IllegalArgumentException("Evento desconocido para settings: " + tipo);
        }
        return ruta;
    }
}
