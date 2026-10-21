package com.fitstore.notification.dto;

import java.util.List;
import java.util.Map;

/** Página del registro con los contadores por estado (sin aplicar el filtro de estado), para los chips del listado. */
public record PaginaEnviosDto(List<EnvioResumenDto> contenido, int pagina, int tamano, long totalElementos,
        int totalPaginas, Map<String, Long> contadores) {
}
