package com.fitstore.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

/** Página de resultados (ADR-015, contracts K6): {@code {contenido, pagina, tamano, totalElementos, totalPaginas}}. */
public record PaginaResponse<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
