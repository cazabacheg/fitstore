package com.fitstore.settings.event;

import java.util.List;
import java.util.Map;

/**
 * Se publica al guardar una sección o una imagen ({@code settings.configuracion.actualizada}).
 * Lleva la configuración pública completa para que quien la recibe no tenga que volver a llamar a settings.
 *
 * @param secciones      IDENTIDAD, TEMA, CONTENIDO, PARAMETROS o IMAGENES
 * @param configuracion  mismo formato que {@code ConfiguracionPublica} de settings-service.yaml
 * @param correosAlertas no es público; el broker es interno
 */
public record ConfiguracionActualizada(int version, List<String> secciones, Map<String, Object> configuracion,
        List<String> correosAlertas) {
}
