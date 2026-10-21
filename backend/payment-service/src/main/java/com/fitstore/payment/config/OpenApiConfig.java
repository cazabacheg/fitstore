package com.fitstore.payment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ajusta el OpenAPI que genera springdoc para que coincida con el contrato escrito a mano
 * ({@code contracts/openapi/payment-service.yaml}, K2): las rutas protegidas declaran {@code bearerAuth}, los parámetros de
 * ruta van en la ruta (no en cada operación) y se quitan los detalles que springdoc agrega por su cuenta
 * ({@code format: int32} y los límites máximos por defecto), que {@code oasdiff} cuenta como cambios incompatibles.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenApiCustomizer ajustesDelContrato() {
        return openApi -> {
            registrarSeguridad(openApi);
            Set<Schema<?>> vistos = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().values().forEach(s -> limpiar(s, vistos));
            }
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((ruta, item) -> {
                moverParametrosDeRuta(item);
                boolean publica = ruta.startsWith("/api/v1/pagos/webhooks") || ruta.startsWith("/internal");
                for (Operation operacion : item.readOperations()) {
                    if (!publica) {
                        operacion.addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
                    }
                    limpiarOperacion(operacion, vistos);
                }
                if (item.getParameters() != null) {
                    item.getParameters().forEach(p -> limpiar(p.getSchema(), vistos));
                }
            });
        };
    }

    private static void registrarSeguridad(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        openApi.getComponents().addSecuritySchemes("bearerAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
    }

    /** El contrato declara el código de pedido o el id una vez por ruta; springdoc los repite en cada operación. */
    private static void moverParametrosDeRuta(PathItem item) {
        Map<String, Parameter> deRuta = new LinkedHashMap<>();
        for (Operation operacion : item.readOperations()) {
            if (operacion.getParameters() == null) {
                continue;
            }
            for (Iterator<Parameter> it = operacion.getParameters().iterator(); it.hasNext();) {
                Parameter p = it.next();
                if ("path".equals(p.getIn())) {
                    deRuta.putIfAbsent(p.getName(), p);
                    it.remove();
                }
            }
            if (operacion.getParameters().isEmpty()) {
                operacion.setParameters(null);
            }
        }
        if (!deRuta.isEmpty()) {
            item.setParameters(new ArrayList<>(deRuta.values()));
        }
    }

    private static void limpiarOperacion(Operation operacion, Set<Schema<?>> vistos) {
        if (operacion.getParameters() != null) {
            operacion.getParameters().forEach(p -> limpiar(p.getSchema(), vistos));
        }
        if (operacion.getRequestBody() != null) {
            limpiarContenido(operacion.getRequestBody().getContent(), vistos);
        }
        if (operacion.getResponses() != null) {
            for (ApiResponse respuesta : operacion.getResponses().values()) {
                limpiarContenido(respuesta.getContent(), vistos);
            }
        }
    }

    private static void limpiarContenido(Content contenido, Set<Schema<?>> vistos) {
        if (contenido != null) {
            contenido.values().forEach(m -> limpiar(m.getSchema(), vistos));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void limpiar(Schema<?> esquema, Set<Schema<?>> vistos) {
        if (esquema == null || !vistos.add(esquema)) {
            return;
        }
        if ("int32".equals(esquema.getFormat())) {
            esquema.setFormat(null);
        }
        if (esquema.getMaxItems() != null && esquema.getMaxItems() == Integer.MAX_VALUE) {
            esquema.setMaxItems(null);
        }
        if (esquema.getMaxLength() != null && esquema.getMaxLength() == Integer.MAX_VALUE) {
            esquema.setMaxLength(null);
        }
        if (esquema.getProperties() != null) {
            ((Map<String, Schema<?>>) (Map) esquema.getProperties()).values().forEach(s -> limpiar(s, vistos));
        }
        limpiar(esquema.getItems(), vistos);
        if (esquema.getAllOf() != null) {
            ((List<Schema<?>>) (List) esquema.getAllOf()).forEach(s -> limpiar(s, vistos));
        }
        if (esquema.getAdditionalProperties() instanceof Schema<?> adicional) {
            limpiar(adicional, vistos);
        }
    }
}
