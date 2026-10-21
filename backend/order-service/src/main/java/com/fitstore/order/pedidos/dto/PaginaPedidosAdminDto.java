package com.fitstore.order.pedidos.dto;

import java.util.List;
import java.util.Map;

/** Página de pedidos del backoffice con los contadores por estado (schema {@code PaginaPedidosAdmin}). */
public record PaginaPedidosAdminDto(List<PedidoAdminResumenDto> contenido, int pagina, int tamano,
        long totalElementos, int totalPaginas, Map<String, Long> contadores) {
}
