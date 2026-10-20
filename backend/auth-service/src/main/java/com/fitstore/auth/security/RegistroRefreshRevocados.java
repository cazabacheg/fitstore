package com.fitstore.auth.security;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Refresh tokens revocados en Redis ({@code auth:refresh:revocado:{jti}}), con la vida restante del token como TTL. */
@Component
public class RegistroRefreshRevocados {

    public RegistroRefreshRevocados(StringRedisTemplate redis) {
    }

    public void revocar(String jti, Duration ttl) {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean estaRevocado(String jti) {
        throw new UnsupportedOperationException("pendiente");
    }
}
