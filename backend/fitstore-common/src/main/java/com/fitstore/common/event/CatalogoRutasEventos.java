package com.fitstore.common.event;

/**
 * Puerto que traduce el {@code tipo} de un evento (p. ej. {@code PedidoPagado}) a su routing key
 * {@code {servicio}.{entidad}.{acción}} (p. ej. {@code order.pedido.pagado}). Cada servicio lo implementa con las
 * direcciones de los canales del AsyncAPI; la librería no inventa rutas.
 */
@FunctionalInterface
public interface CatalogoRutasEventos {

    /** @throws IllegalArgumentException si el tipo no está en el catálogo */
    String routingKey(String tipo);
}
