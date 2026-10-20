package com.fitstore.order.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros propios de order (micro 10.5, bloque {@code fitstore.order}).
 *
 * @param carrito           límites y duración de los carritos
 * @param codigoPrefijo     prefijo del código legible del pedido ({@code FS-000123})
 * @param settingsCacheTtl  duración del caché de los parámetros de settings
 * @param expiradorCron     cuándo corre el planificador que expira los pedidos sin pago
 * @param defaults          valores por omisión si settings no responde y no hay caché
 */
@ConfigurationProperties(prefix = "fitstore.order")
public record PropiedadesOrder(Carrito carrito, String codigoPrefijo, Duration settingsCacheTtl,
        String expiradorCron, Defaults defaults) {

    /** Invariante 6: máximo de líneas y de unidades por línea; el carrito anónimo dura menos que el del cliente. */
    public record Carrito(int maxLineas, int maxUnidadesLinea, Duration ttlAnonimo, Duration ttlCliente) {
    }

    public record Defaults(int expiracionPedidoMinutos) {
    }
}
