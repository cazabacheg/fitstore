package com.fitstore.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de la seguridad base de los servicios (SPEC-0a I3).
 *
 * @param habilitado  activa el resource server de fitstore-common (por defecto sí)
 * @param jwksArchivo ruta de un archivo JWKS con la clave pública; solo para dev, mientras auth no firma (SPEC-0a I2).
 *                    Si no se indica, se valida con {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}
 */
@ConfigurationProperties(prefix = "fitstore.security")
public record FitstoreSecurityProperties(Boolean habilitado, String jwksArchivo) {
}
