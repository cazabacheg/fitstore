package com.fitstore.auth.security;

import com.fitstore.auth.config.PropiedadesAuth;
import com.fitstore.auth.model.TipoCuenta;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Intentos fallidos de login en Redis (micro 3.2): contador {@code auth:login:fallos:{tipo}:{email}} y marca de bloqueo
 * {@code auth:login:bloqueo:{tipo}:{email}}, ambos de 15 min e independientes para clientes y staff. Si Redis no
 * responde, el login sigue funcionando sin bloqueo temporal y se registra una advertencia.
 */
@Component
public class RegistroIntentosLogin {

    private static final Logger LOG = LoggerFactory.getLogger(RegistroIntentosLogin.class);

    private final StringRedisTemplate redis;
    private final int maximo;
    private final Duration duracion;

    public RegistroIntentosLogin(StringRedisTemplate redis, PropiedadesAuth propiedades) {
        this.redis = redis;
        this.maximo = propiedades.bloqueo().intentos();
        this.duracion = propiedades.bloqueo().duracion();
    }

    public boolean estaBloqueado(TipoCuenta tipo, String email) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(claveBloqueo(tipo, email)));
        } catch (DataAccessException e) {
            LOG.warn("Redis no responde: se ingresa sin consultar el bloqueo de {}", tipo, e);
            return false;
        }
    }

    /** Suma un fallo; al llegar al máximo crea el bloqueo. */
    public void registrarFallo(TipoCuenta tipo, String email) {
        try {
            String clave = claveFallos(tipo, email);
            Long fallos = redis.opsForValue().increment(clave);
            if (fallos != null && fallos == 1) {
                redis.expire(clave, duracion);
            }
            if (fallos != null && fallos >= maximo) {
                redis.opsForValue().set(claveBloqueo(tipo, email), "1", duracion);
            }
        } catch (DataAccessException e) {
            LOG.warn("Redis no responde: no se registra el intento fallido de {}", tipo, e);
        }
    }

    /** Borra el contador tras un ingreso correcto. */
    public void limpiar(TipoCuenta tipo, String email) {
        try {
            redis.delete(claveFallos(tipo, email));
        } catch (DataAccessException e) {
            LOG.warn("Redis no responde: no se borra el contador de fallos de {}", tipo, e);
        }
    }

    private static String claveFallos(TipoCuenta tipo, String email) {
        return "auth:login:fallos:" + tipo.name().toLowerCase(Locale.ROOT) + ":" + email;
    }

    private static String claveBloqueo(TipoCuenta tipo, String email) {
        return "auth:login:bloqueo:" + tipo.name().toLowerCase(Locale.ROOT) + ":" + email;
    }
}
