package com.fitstore.settings.configuracion.service;

import com.fitstore.settings.config.PropiedadesSettings;
import com.fitstore.settings.configuracion.dto.ConfiguracionPublicaDto;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Caché de la configuración pública en Redis (clave {@code settings:public}, ADR-020). Si Redis no responde, nunca
 * falla: se lee de la base de datos (CA-3-37).
 */
@Component
public class CachePublica {

    public static final String CLAVE = "settings:public";

    private static final Logger LOG = LoggerFactory.getLogger(CachePublica.class);

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final PropiedadesSettings propiedades;

    public CachePublica(StringRedisTemplate redis, JsonMapper json, PropiedadesSettings propiedades) {
        this.redis = redis;
        this.json = json;
        this.propiedades = propiedades;
    }

    public Optional<ConfiguracionPublicaDto> obtener() {
        try {
            String texto = redis.opsForValue().get(CLAVE);
            return texto == null ? Optional.empty() : Optional.of(json.readValue(texto, ConfiguracionPublicaDto.class));
        } catch (RuntimeException e) {
            LOG.warn("No se pudo leer la caché de configuración pública; se usa la base de datos: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void guardar(ConfiguracionPublicaDto configuracion) {
        try {
            redis.opsForValue().set(CLAVE, json.writeValueAsString(configuracion), propiedades.cacheTtl().publica());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo guardar la configuración pública en la caché: {}", e.getMessage());
        }
    }

    public void invalidar() {
        try {
            redis.delete(CLAVE);
        } catch (RuntimeException e) {
            LOG.warn("No se pudo invalidar la caché de configuración pública: {}", e.getMessage());
        }
    }
}
