package com.fitstore.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del almacenamiento S3 compatible ({@code fitstore.storage.*}). */
@ConfigurationProperties(prefix = "fitstore.storage")
public class StorageProperties {

    /** Activa el adaptador S3 del servicio. */
    private boolean habilitado;
    private String endpoint;
    private String region = "garage";
    private String accessKey;
    private String secretKey;
    private String bucketPublico = "fitstore-media";
    private String bucketPrivado = "fitstore-privado";
    /** Base pública de los medios (p. ej. https://tienda.midominio.duckdns.org/media). */
    private String mediaBaseUrl;
    /** Garage exige acceso por ruta (no por subdominio de bucket). */
    private boolean pathStyle = true;

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucketPublico() {
        return bucketPublico;
    }

    public void setBucketPublico(String bucketPublico) {
        this.bucketPublico = bucketPublico;
    }

    public String getBucketPrivado() {
        return bucketPrivado;
    }

    public void setBucketPrivado(String bucketPrivado) {
        this.bucketPrivado = bucketPrivado;
    }

    public String getMediaBaseUrl() {
        return mediaBaseUrl;
    }

    public void setMediaBaseUrl(String mediaBaseUrl) {
        this.mediaBaseUrl = mediaBaseUrl;
    }

    public boolean isPathStyle() {
        return pathStyle;
    }

    public void setPathStyle(boolean pathStyle) {
        this.pathStyle = pathStyle;
    }
}
