package com.fitstore.delivery.dto;

import java.util.List;
import java.util.Map;

/** Página de entregas con los contadores por estado de las pestañas (CA-6-31). */
public record PaginaEntregasDto(List<EntregaResumenDto> contenido, int pagina, int tamano, long totalElementos,
        int totalPaginas, Map<String, Long> contadores) {
}
