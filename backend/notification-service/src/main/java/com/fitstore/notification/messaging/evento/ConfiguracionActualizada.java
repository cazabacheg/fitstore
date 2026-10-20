package com.fitstore.notification.messaging.evento;

import java.util.List;
import java.util.Map;

/** Datos del evento {@code ConfiguracionActualizada} (routing key {@code settings.configuracion.actualizada}), según el AsyncAPI. */
public record ConfiguracionActualizada(int version, List<String> secciones, Map<String, Object> configuracion,
        List<String> correosAlertas) {
}
