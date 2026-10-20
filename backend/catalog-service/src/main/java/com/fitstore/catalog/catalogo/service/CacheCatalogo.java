package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.config.CatalogoProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Caché de catalog en Redis (micro 3.2, ADR-020). Todas las claves llevan el prefijo {@code catalog:}.
 *
 * <p>Si Redis no responde (o la caché está apagada) todo se lee de la base de datos: ninguna operación lanza error.
 * El stock nunca se guarda aquí.
 */
@Component
public class CacheCatalogo {

    public static final String ARBOL_CATEGORIAS = "catalog:categorias:arbol";
    public static final String DESTACADOS = "catalog:destacados";
    public static final String DESCUENTOS_GLOBAL = "catalog:descuentos:global";
    public static final String PREFIJO_PRODUCTO = "catalog:producto:";

    private static final Logger LOG = LoggerFactory.getLogger(CacheCatalogo.class);

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final boolean habilitada;
    private final Duration ttl;

    public CacheCatalogo(StringRedisTemplate redis, JsonMapper json, CatalogoProperties propiedades) {
        this.redis = redis;
        this.json = json;
        this.habilitada = propiedades.cache().habilitado();
        this.ttl = propiedades.cache().ttl();
    }

    public static String clavePorSlug(String slug) {
        return PREFIJO_PRODUCTO + slug;
    }

    /** Devuelve el valor de la caché o lo carga de la base de datos y lo guarda con el TTL de 10 minutos. */
    public <T> T obtener(String clave, TypeReference<T> tipo, Supplier<T> cargador) {
        Optional<T> guardado = leer(clave, tipo);
        if (guardado.isPresent()) {
            return guardado.get();
        }
        T valor = cargador.get();
        if (valor != null) {
            guardar(clave, valor, ttl);
        }
        return valor;
    }

    public <T> Optional<T> leer(String clave, TypeReference<T> tipo) {
        if (!habilitada) {
            return Optional.empty();
        }
        try {
            String contenido = redis.opsForValue().get(clave);
            return contenido == null ? Optional.empty() : Optional.ofNullable(json.readValue(contenido, tipo));
        } catch (RuntimeException e) {
            LOG.warn("Caché no disponible al leer {}: {}", clave, e.getMessage());
            return Optional.empty();
        }
    }

    /** @return {@code true} si quedó guardado en Redis */
    public boolean guardar(String clave, Object valor, Duration duracion) {
        if (!habilitada) {
            return false;
        }
        try {
            redis.opsForValue().set(clave, json.writeValueAsString(valor), duracion);
            return true;
        } catch (RuntimeException e) {
            LOG.warn("Caché no disponible al guardar {}: {}", clave, e.getMessage());
            return false;
        }
    }

    public void invalidar(String... claves) {
        if (!habilitada) {
            return;
        }
        try {
            redis.delete(List.of(claves));
        } catch (RuntimeException e) {
            LOG.warn("Caché no disponible al invalidar {}: {}", String.join(",", claves), e.getMessage());
        }
    }

    /** Invalida el detalle de un producto (por su slug actual y, si cambió, por el anterior). */
    public void invalidarProducto(String... slugs) {
        String[] claves = new String[slugs.length];
        for (int i = 0; i < slugs.length; i++) {
            claves[i] = clavePorSlug(slugs[i]);
        }
        invalidar(claves);
        invalidar(DESTACADOS);
    }

    /** Invalida todos los detalles de producto (cambia la política global de descuentos, una categoría, etc.). */
    public void invalidarTodosLosProductos() {
        if (!habilitada) {
            return;
        }
        try {
            List<String> claves = new ArrayList<>();
            try (Cursor<String> cursor = redis.scan(ScanOptions.scanOptions().match(PREFIJO_PRODUCTO + "*").count(200).build())) {
                cursor.forEachRemaining(claves::add);
            }
            if (!claves.isEmpty()) {
                redis.delete(claves);
            }
            redis.delete(DESTACADOS);
        } catch (RuntimeException e) {
            LOG.warn("Caché no disponible al invalidar los productos: {}", e.getMessage());
        }
    }
}
