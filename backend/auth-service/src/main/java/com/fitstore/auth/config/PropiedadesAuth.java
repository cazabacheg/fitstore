package com.fitstore.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de auth (micro 10.5, {@code fitstore.auth.*}).
 *
 * @param issuer                emisor ({@code iss}) de los JWT
 * @param accessTtl             vida del access token
 * @param refreshTtl            vida del refresh token
 * @param bcryptStrength        fuerza de BCrypt (10 por defecto, ~100 ms por hash)
 * @param bloqueo               bloqueo temporal por intentos fallidos
 * @param tokenRecuperacionTtl  vida del enlace de recuperación
 * @param tokenInvitacionTtl    vida del enlace de invitación de staff
 * @param claves                par RSA con el que se firman los JWT
 * @param cookie                cookie del refresh token (A4)
 * @param enlaces               URL base de cada frontend, para los enlaces de los correos (A3)
 */
@ConfigurationProperties(prefix = "fitstore.auth")
public record PropiedadesAuth(
        String issuer,
        @DefaultValue("15m") Duration accessTtl,
        @DefaultValue("7d") Duration refreshTtl,
        @DefaultValue("10") int bcryptStrength,
        @DefaultValue Bloqueo bloqueo,
        @DefaultValue("30m") Duration tokenRecuperacionTtl,
        @DefaultValue("24h") Duration tokenInvitacionTtl,
        @DefaultValue Claves claves,
        @DefaultValue Cookie cookie,
        @DefaultValue Enlaces enlaces) {

    /** @param intentos fallos consecutivos que bloquean · @param duracion tiempo de bloqueo y de vida del contador */
    public record Bloqueo(@DefaultValue("5") int intentos, @DefaultValue("15m") Duration duracion) {
    }

    /**
     * @param privada        ruta del PEM PKCS#8 de la clave privada
     * @param publica        ruta del PEM X.509 de la clave pública
     * @param kid            identificador de la clave en el JWKS
     * @param generarSiFaltan si los archivos no existen, genera un par al arrancar (solo en el perfil dev)
     */
    public record Claves(String privada, String publica, @DefaultValue("fs-1") String kid,
            @DefaultValue("false") boolean generarSiFaltan) {
    }

    /** Cookie {@code fs_refresh}: {@code HttpOnly}; {@code Secure} salvo en dev (HTTP local). */
    public record Cookie(@DefaultValue("fs_refresh") String nombre, @DefaultValue("true") boolean secure,
            @DefaultValue("Strict") String sameSite, @DefaultValue("/api/v1/auth") String path) {
    }

    /** @param storefront URL de la tienda · @param backoffice URL del backoffice */
    public record Enlaces(String storefront, String backoffice) {
    }
}
