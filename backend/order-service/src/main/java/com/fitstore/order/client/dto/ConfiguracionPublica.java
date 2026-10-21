package com.fitstore.order.client.dto;

import java.util.List;

/** Lo que order usa de {@code GET /settings/public}: modalidades, expiración y la tienda física (para el recojo). */
public record ConfiguracionPublica(Contenido contenido, Parametros parametros) {

    public record Contenido(String horario, TiendaFisica tiendaFisica) {
    }

    public record TiendaFisica(String direccion, String ubigeo, String ubigeoNombre, String referencia) {
    }

    public record Parametros(List<String> modalidadesEntrega, int expiracionPedidoMinutos,
            int diasReembolsoPostventa) {
    }
}
