package com.fitstore.auth.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Refresh tokens revocados en Redis ({@code auth:refresh:revocado:{jti}}), con la vida restante del token como TTL. Para
 * el margen de reuso (CA-1-40) guarda también, por unos segundos, qué navegador rotó cada token y qué token lo
 * reemplazó ({@code auth:refresh:rotado:{jti}}), y los tokens cerrados con logout ({@code auth:refresh:cerrado:{jti}}).
 */
@Component
public class RegistroRefreshRevocados {

    private static final String PREFIJO = "auth:refresh:revocado:";
    private static final String PREFIJO_ROTADO = "auth:refresh:rotado:";
    private static final String PREFIJO_CERRADO = "auth:refresh:cerrado:";

    private final StringRedisTemplate redis;

    public RegistroRefreshRevocados(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * Marca el {@code jti} como revocado.
     *
     * @return {@code true} si lo revocó ahora; {@code false} si ya estaba revocado (dos usos del mismo refresh)
     */
    public boolean revocar(String jti, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return true;
        }
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(PREFIJO + jti, "1", ttl));
    }

    public boolean estaRevocado(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIJO + jti));
    }

    /** Recuerda durante {@code margen} que el navegador con esa huella rotó {@code jti} y recibió {@code sucesor}. */
    public void marcarRotado(String jti, String huella, String sucesor, Duration margen) {
        redis.opsForValue().set(PREFIJO_ROTADO + jti, huella + " " + sucesor, margen);
    }

    /**
     * {@code true} si {@code jti} se rotó hace menos del margen desde el mismo navegador y el token que lo reemplazó no
     * se cerró con logout.
     */
    public boolean rotadoRecientemente(String jti, String huella) {
        String marca = redis.opsForValue().get(PREFIJO_ROTADO + jti);
        if (marca == null) {
            return false;
        }
        String[] partes = marca.split(" ", 2);
        return partes.length == 2 && partes[0].equals(huella) && !Boolean.TRUE.equals(redis.hasKey(PREFIJO_CERRADO + partes[1]));
    }

    /** Marca {@code jti} como cerrado con logout durante {@code margen}, para que el margen de reuso no lo reviva. */
    public void marcarCerrado(String jti, Duration margen) {
        redis.opsForValue().set(PREFIJO_CERRADO + jti, "1", margen);
    }
}
