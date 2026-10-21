package com.fitstore.auth.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Refresh tokens revocados en Redis ({@code auth:refresh:revocado:{jti}}), con la vida restante del token como TTL. */
@Component
public class RegistroRefreshRevocados {

    private static final String PREFIJO = "auth:refresh:revocado:";

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
}
