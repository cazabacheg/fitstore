package com.fitstore.notification.client;

import com.fitstore.notification.config.PropiedadesNotificacion;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Llamada a settings, solo al arrancar sin copia de la marca (micro D3). */
@Component
public class SettingsClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() { };

    private final RestClient cliente;
    private final String base;

    public SettingsClient(RestClient clienteSaliente, PropiedadesNotificacion propiedades) {
        this.cliente = clienteSaliente;
        this.base = propiedades.settingsUrl() == null ? "" : propiedades.settingsUrl().replaceAll("/+$", "");
    }

    /** @return la configuración pública (el {@code data} de {@code GET /api/v1/settings/public}) */
    @SuppressWarnings("unchecked")
    public Map<String, Object> obtenerPublica() {
        if (base.isBlank()) {
            throw new IllegalStateException("fitstore.notificacion.settings-url no está configurada");
        }
        Map<String, Object> respuesta = cliente.get().uri(base + "/api/v1/settings/public").retrieve().body(MAPA);
        if (respuesta == null || !(respuesta.get("data") instanceof Map<?, ?> data)) {
            throw new IllegalStateException("Settings respondió sin configuración pública");
        }
        return (Map<String, Object>) data;
    }
}
