package com.fitstore.gateway.seguridad;

import com.fitstore.gateway.config.GatewayProperties;
import com.fitstore.gateway.web.RespuestasError;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seguridad y CORS del gateway (infraestructura 4.3): valida el JWT (firma y vencimiento) cuando la petición trae
 * {@code Authorization}; si es inválido, 401 sin llegar al servicio. Las rutas públicas pasan sin token y la
 * autorización por rol la hace cada servicio (ADR-011).
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayProperties.class)
public class SeguridadJwtConfig {

    @Bean
    SecurityWebFilterChain gatewaySecurity(ServerHttpSecurity http, JsonMapper json) {
        ServerAuthenticationEntryPoint noAutenticado = (intercambio, error) -> RespuestasError.escribir(intercambio, json,
                HttpStatus.UNAUTHORIZED, "Tu sesión venció, vuelve a ingresar", "NO_AUTENTICADO");
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(a -> a.anyExchange().permitAll())
                .oauth2ResourceServer(o -> o.authenticationEntryPoint(noAutenticado).jwt(Customizer.withDefaults()))
                .build();
    }

    /** Dev: valida con la clave pública de un archivo JWKS (SPEC-0a I2); sin la propiedad se usa {@code jwk-set-uri}. */
    @Bean
    @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
    @ConditionalOnProperty(prefix = "fitstore.security", name = "jwks-archivo")
    ReactiveJwtDecoder jwtDecoderDeArchivo(@org.springframework.beans.factory.annotation.Value("${fitstore.security.jwks-archivo}") String archivo) {
        try {
            JWKSet claves = JWKSet.parse(Files.readString(Path.of(archivo), StandardCharsets.UTF_8));
            RSAKey rsa = claves.getKeys().stream()
                    .filter(RSAKey.class::isInstance)
                    .map(RSAKey.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("El JWKS de " + archivo + " no tiene clave RSA"));
            return NimbusReactiveJwtDecoder.withPublicKey(rsa.toRSAPublicKey()).build();
        } catch (IOException | ParseException | JOSEException e) {
            throw new IllegalStateException("No se pudo leer el JWKS " + archivo, e);
        }
    }

    /** CORS: solo los orígenes del storefront y del backoffice (por configuración de entorno). */
    @Bean
    CorsConfigurationSource corsConfigurationSource(GatewayProperties propiedades) {
        var cors = propiedades.cors();
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(cors.origenes());
        configuracion.setAllowedMethods(cors.metodos());
        configuracion.setAllowedHeaders(cors.cabeceras());
        configuracion.setExposedHeaders(java.util.List.of("X-Request-Id", "Retry-After", "Location"));
        configuracion.setAllowCredentials(true);
        configuracion.setMaxAge(cors.maxAgeSegundos());
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
