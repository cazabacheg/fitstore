package com.fitstore.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI que genera el servicio (springdoc, 3.0). {@code scripts/contrato.sh} lo compara con
 * {@code contracts/openapi/auth-service.yaml} con oasdiff (contracts K2).
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "FitStore — auth-service", version = "1.0.0"))
@SecuritySchemes({
    @SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
            description = "Access token RS256 emitido por auth-service (15 min). Va en memoria en el navegador."),
    @SecurityScheme(name = "refreshCookie", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE, paramName = "fs_refresh",
            description = "Refresh token en cookie HttpOnly, Secure, SameSite=Strict, Path=/api/v1/auth (7 días, rota en cada uso).")
})
public class OpenApiConfig {

    /**
     * Deja el documento como lo escribe el contrato: los enteros sin {@code format: int32} (el contrato solo pone
     * {@code int64} cuando hace falta) y los parámetros de ruta declarados una vez por ruta.
     */
    @Bean
    OpenApiCustomizer contratoDeAuth() {
        return (OpenAPI api) -> {
            if (api.getComponents() != null && api.getComponents().getSchemas() != null) {
                api.getComponents().getSchemas().values().forEach(OpenApiConfig::quitarInt32);
            }
            if (api.getPaths() != null) {
                api.getPaths().values().forEach(OpenApiConfig::ordenar);
            }
        };
    }

    private static void ordenar(PathItem ruta) {
        Map<String, Parameter> deRuta = new LinkedHashMap<>();
        for (Operation operacion : ruta.readOperations()) {
            if (operacion.getParameters() == null) {
                continue;
            }
            List<Parameter> restantes = new ArrayList<>();
            for (Parameter p : operacion.getParameters()) {
                if ("path".equals(p.getIn())) {
                    deRuta.putIfAbsent(p.getName(), p);
                } else {
                    if (p.getSchema() != null) {
                        quitarInt32(p.getSchema());
                    }
                    restantes.add(p);
                }
            }
            operacion.setParameters(restantes.isEmpty() ? null : restantes);
        }
        if (!deRuta.isEmpty()) {
            deRuta.values().forEach(p -> quitarInt32(p.getSchema()));
            ruta.setParameters(new ArrayList<>(deRuta.values()));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void quitarInt32(Schema esquema) {
        if (esquema == null) {
            return;
        }
        if ("integer".equals(esquema.getType()) && "int32".equals(esquema.getFormat())) {
            esquema.setFormat(null);
        }
        if (esquema.getProperties() != null) {
            ((Map<String, Schema>) esquema.getProperties()).values().forEach(OpenApiConfig::quitarInt32);
        }
        if (esquema.getItems() != null) {
            quitarInt32(esquema.getItems());
        }
        if (esquema.getAllOf() != null) {
            ((List<Schema>) esquema.getAllOf()).forEach(OpenApiConfig::quitarInt32);
        }
    }
}
