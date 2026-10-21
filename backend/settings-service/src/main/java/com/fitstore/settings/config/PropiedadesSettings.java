package com.fitstore.settings.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Parámetros técnicos de settings ({@code fitstore.settings.*}, micro 10.5). Los valores por defecto son los del micro,
 * así que el servicio también funciona sin ellos (por ejemplo, en las pruebas).
 */
@ConfigurationProperties(prefix = "fitstore.settings")
public record PropiedadesSettings(
        @DefaultValue CacheTtl cacheTtl,
        @DefaultValue Imagenes imagenes,
        @DefaultValue Reclamos reclamos) {

    /** Vigencia de las cachés en Redis. */
    public record CacheTtl(@DefaultValue("1h") Duration publica, @DefaultValue("24h") Duration ubigeo) {
    }

    /** Límites de las imágenes de la tienda. */
    public record Imagenes(
            @DefaultValue("2MB") DataSize logoMax,
            @DefaultValue("5MB") DataSize landingMax,
            @DefaultValue("1600") int landingAncho,
            @DefaultValue("5") int landingMaxCantidad) {
    }

    /** Plazos y serie del Libro de Reclamaciones. */
    public record Reclamos(
            @DefaultValue("15") int plazoDiasHabiles,
            @DefaultValue("3") int porVencerDias,
            @DefaultValue("RC") String serie) {
    }
}
