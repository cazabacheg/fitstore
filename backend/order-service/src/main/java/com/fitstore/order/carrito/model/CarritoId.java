package com.fitstore.order.carrito.model;

import java.util.UUID;

/**
 * Identifica un carrito en Redis (micro order 3.2): de un cliente ({@code order:carrito:cli:{clienteId}}) o anónimo
 * ({@code order:carrito:anon:{carritoId}}, con el UUID que genera el navegador).
 */
public record CarritoId(TipoCarrito tipo, String valor) {

    private static final String PREFIJO = "order:carrito:";

    public static CarritoId cliente(UUID clienteId) {
        return new CarritoId(TipoCarrito.CLIENTE, clienteId.toString());
    }

    public static CarritoId anonimo(String carritoId) {
        return new CarritoId(TipoCarrito.ANONIMO, carritoId);
    }

    /** Clave del hash {@code varianteId → cantidad}. */
    public String clave() {
        return PREFIJO + (tipo == TipoCarrito.CLIENTE ? "cli:" : "anon:") + valor;
    }

    /** Clave de la última cotización (D2), con el mismo TTL del carrito. */
    public String claveCotizacion() {
        return clave() + ":cotizacion";
    }
}
