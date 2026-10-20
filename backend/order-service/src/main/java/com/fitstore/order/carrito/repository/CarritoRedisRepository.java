package com.fitstore.order.carrito.repository;

import com.fitstore.order.carrito.model.CarritoId;
import com.fitstore.order.carrito.model.TipoCarrito;
import com.fitstore.order.config.PropiedadesOrder;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/** {@link CarritoRepository} sobre un hash de Redis por carrito; el TTL de Redis expira los carritos abandonados. */
@Repository
public class CarritoRedisRepository implements CarritoRepository {

    private final StringRedisTemplate redis;
    private final PropiedadesOrder propiedades;

    public CarritoRedisRepository(StringRedisTemplate redis, PropiedadesOrder propiedades) {
        this.redis = redis;
        this.propiedades = propiedades;
    }

    @Override
    public Map<Long, Integer> obtenerLineas(CarritoId carrito) {
        Map<Object, Object> hash = redis.opsForHash().entries(carrito.clave());
        Map<Long, Integer> lineas = new LinkedHashMap<>();
        hash.forEach((variante, cantidad) ->
                lineas.put(Long.valueOf((String) variante), Integer.valueOf((String) cantidad)));
        return lineas;
    }

    @Override
    public void establecerLinea(CarritoId carrito, long varianteId, int cantidad) {
        redis.opsForHash().put(carrito.clave(), Long.toString(varianteId), Integer.toString(cantidad));
        renovar(carrito);
    }

    @Override
    public void quitarLinea(CarritoId carrito, long varianteId) {
        redis.opsForHash().delete(carrito.clave(), Long.toString(varianteId));
        if (Boolean.TRUE.equals(redis.hasKey(carrito.clave()))) {
            renovar(carrito);
        } else {
            redis.delete(carrito.claveCotizacion());
        }
    }

    @Override
    public void vaciar(CarritoId carrito) {
        redis.delete(carrito.clave());
        redis.delete(carrito.claveCotizacion());
    }

    @Override
    public Optional<Duration> ttl(CarritoId carrito) {
        Long segundos = redis.getExpire(carrito.clave());
        return segundos == null || segundos < 0 ? Optional.empty() : Optional.of(Duration.ofSeconds(segundos));
    }

    @Override
    public void guardarCotizacion(CarritoId carrito, String cotizacionJson) {
        redis.opsForValue().set(carrito.claveCotizacion(), cotizacionJson, ttlDe(carrito));
    }

    @Override
    public Optional<String> leerCotizacion(CarritoId carrito) {
        return Optional.ofNullable(redis.opsForValue().get(carrito.claveCotizacion()));
    }

    @Override
    public Map<Long, Integer> fusionar(CarritoId anonimo, CarritoId cliente) {
        Map<Long, Integer> resultado = new LinkedHashMap<>(obtenerLineas(cliente));
        obtenerLineas(anonimo).forEach((variante, cantidad) -> resultado.merge(variante, cantidad, Integer::sum));
        if (!resultado.isEmpty()) {
            resultado.forEach((variante, cantidad) ->
                    redis.opsForHash().put(cliente.clave(), Long.toString(variante), Integer.toString(cantidad)));
            renovar(cliente);
        }
        vaciar(anonimo);
        redis.delete(cliente.claveCotizacion());
        return resultado;
    }

    private void renovar(CarritoId carrito) {
        Duration ttl = ttlDe(carrito);
        redis.expire(carrito.clave(), ttl);
        redis.expire(carrito.claveCotizacion(), ttl);
    }

    private Duration ttlDe(CarritoId carrito) {
        PropiedadesOrder.Carrito config = propiedades.carrito();
        return carrito.tipo() == TipoCarrito.CLIENTE ? config.ttlCliente() : config.ttlAnonimo();
    }
}
