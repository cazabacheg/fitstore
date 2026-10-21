package com.fitstore.catalog.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades del módulo catálogo ({@code fitstore.catalogo.*}, micro 10.5). Los valores por defecto son los del
 * config-repo, para que el servicio también arranque en las pruebas sin config-server.
 *
 * @param imagenes   límite de imágenes por producto y anchos de las versiones generadas
 * @param descuentos máximo de tramos y porcentaje máximo
 * @param cotizacion máximo de líneas por cotización
 * @param cache      caché Redis: se puede apagar (pruebas) y fija el TTL (10 min)
 * @param demo       carga de las fotos de demostración a Garage (solo con los perfiles dev o demo)
 * @param cargaMasiva vigencia de la validación y máximo de filas
 */
@ConfigurationProperties(prefix = "fitstore.catalogo")
public record CatalogoProperties(Imagenes imagenes, Descuentos descuentos, Cotizacion cotizacion, Cache cache, Demo demo,
        CargaMasiva cargaMasiva) {

    public CatalogoProperties {
        imagenes = imagenes == null ? new Imagenes(null, null) : imagenes;
        descuentos = descuentos == null ? new Descuentos(null, null) : descuentos;
        cotizacion = cotizacion == null ? new Cotizacion(null) : cotizacion;
        cache = cache == null ? new Cache(null, null) : cache;
        demo = demo == null ? new Demo(null) : demo;
        cargaMasiva = cargaMasiva == null ? new CargaMasiva(null, null) : cargaMasiva;
    }

    public record Imagenes(Integer maxPorProducto, List<Integer> tamanos) {
        public Imagenes {
            maxPorProducto = maxPorProducto == null ? 10 : maxPorProducto;
            tamanos = tamanos == null ? List.of(1200, 400) : List.copyOf(tamanos);
        }

        /** Ancho de la versión grande (detalle). */
        public int grande() {
            return tamanos.get(0);
        }

        /** Ancho de la miniatura (tarjetas). */
        public int miniatura() {
            return tamanos.get(1);
        }
    }

    public record Descuentos(Integer maxTramos, Integer porcentajeMax) {
        public Descuentos {
            maxTramos = maxTramos == null ? 10 : maxTramos;
            porcentajeMax = porcentajeMax == null ? 50 : porcentajeMax;
        }
    }

    public record Cotizacion(Integer maxLineas) {
        public Cotizacion {
            maxLineas = maxLineas == null ? 50 : maxLineas;
        }
    }

    public record Cache(Boolean habilitado, Duration ttl) {
        public Cache {
            habilitado = habilitado == null || habilitado;
            ttl = ttl == null ? Duration.ofMinutes(10) : ttl;
        }
    }

    /** Límites de la carga masiva (micro 4.4.1): 2 000 filas, 5 MB y la validación vale 30 minutos. */
    public record CargaMasiva(Duration vigencia, Integer maxFilas) {
        public CargaMasiva {
            vigencia = vigencia == null ? Duration.ofMinutes(30) : vigencia;
            maxFilas = maxFilas == null ? 2000 : maxFilas;
        }
    }

    public record Demo(Boolean subirImagenes) {
        public Demo {
            subirImagenes = subirImagenes == null || subirImagenes;
        }
    }
}
