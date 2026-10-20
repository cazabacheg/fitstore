package com.fitstore.order.carrito.repository;

import com.fitstore.order.carrito.model.CarritoId;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Carritos en Redis (micro order 3.2). Solo guarda {@code varianteId → cantidad} y la última cotización como JSON
 * (D1 y D2). Los límites de líneas y unidades los valida {@code CarritoService} (invariante 6).
 */
public interface CarritoRepository {

    /** Líneas del carrito; vacío si no existe o expiró. */
    Map<Long, Integer> obtenerLineas(CarritoId carrito);

    /** Agrega la línea o cambia su cantidad, y renueva el TTL del carrito y de su cotización. */
    void establecerLinea(CarritoId carrito, long varianteId, int cantidad);

    /** Quita una línea; al quitar la última desaparece el carrito con su cotización. */
    void quitarLinea(CarritoId carrito, long varianteId);

    /** Elimina el carrito y su cotización. */
    void vaciar(CarritoId carrito);

    /** Tiempo que le queda al carrito; vacío si no existe. */
    Optional<Duration> ttl(CarritoId carrito);

    /** Guarda la última cotización (JSON) con el mismo TTL del carrito. */
    void guardarCotizacion(CarritoId carrito, String cotizacionJson);

    Optional<String> leerCotizacion(CarritoId carrito);

    /**
     * Fusión al iniciar sesión: suma las cantidades por variante en el carrito del cliente, borra el anónimo y las
     * dos cotizaciones (quedaron desactualizadas). Devuelve las líneas resultantes.
     */
    Map<Long, Integer> fusionar(CarritoId anonimo, CarritoId cliente);
}
