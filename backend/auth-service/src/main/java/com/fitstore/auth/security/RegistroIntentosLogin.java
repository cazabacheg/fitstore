package com.fitstore.auth.security;

import com.fitstore.auth.config.PropiedadesAuth;
import com.fitstore.auth.model.TipoCuenta;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Intentos fallidos de login en Redis (micro 3.2): contador {@code auth:login:fallos:{tipo}:{email}} y marca de bloqueo
 * {@code auth:login:bloqueo:{tipo}:{email}}, ambos de 15 min e independientes para clientes y staff.
 */
@Component
public class RegistroIntentosLogin {

    public RegistroIntentosLogin(StringRedisTemplate redis, PropiedadesAuth propiedades) {
    }

    public boolean estaBloqueado(TipoCuenta tipo, String email) {
        throw new UnsupportedOperationException("pendiente");
    }

    /** Suma un fallo; al llegar al máximo crea el bloqueo. */
    public void registrarFallo(TipoCuenta tipo, String email) {
        throw new UnsupportedOperationException("pendiente");
    }

    /** Borra el contador tras un ingreso correcto. */
    public void limpiar(TipoCuenta tipo, String email) {
        throw new UnsupportedOperationException("pendiente");
    }
}
