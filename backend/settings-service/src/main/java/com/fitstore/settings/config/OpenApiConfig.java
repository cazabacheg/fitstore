package com.fitstore.settings.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI de settings. El contrato ({@code contracts/openapi/settings-service.yaml}) es la fuente: el
 * esquema {@code bearerAuth} de los endpoints de staff y los ajustes de {@link #alinearConElContrato()} hacen que lo
 * que genera springdoc diga lo mismo que el contrato en lo que Java no puede expresar (formatos, nulos, longitudes
 * implícitas), para que {@code scripts/contrato.sh} no vea cambios incompatibles.
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

    /** Parámetros de ruta que el contrato declara una sola vez por ruta, no por operación. */
    private static final Set<String> PARAMETROS_DE_RUTA_COMPARTIDOS = Set.of("id", "tipo");

    @Bean
    OpenApiCustomizer alinearConElContrato() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().forEach((nombre, esquema) -> {
                    if (nombre.endsWith("Request")) {
                        marcarOpcionalesComoNulos(esquema, openApi.getComponents().getSchemas());
                    }
                    ajustar(esquema, new HashSet<>());
                });
            }
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(this::ajustarRuta);
            }
        };
    }

    private void ajustarRuta(PathItem ruta) {
        List<Operation> operaciones = ruta.readOperations();
        operaciones.forEach(op -> {
            if (op.getParameters() != null) {
                op.getParameters().forEach(p -> {
                    if (p.getSchema() != null) {
                        ajustar(p.getSchema(), new HashSet<>());
                    }
                });
            }
        });
        // El contrato declara {id} y {tipo} en la ruta; springdoc los pone en cada operación
        for (String nombre : PARAMETROS_DE_RUTA_COMPARTIDOS) {
            List<Parameter> encontrados = new ArrayList<>();
            for (Operation op : operaciones) {
                if (op.getParameters() == null) {
                    continue;
                }
                op.getParameters().stream()
                        .filter(p -> "path".equals(p.getIn()) && nombre.equals(p.getName()))
                        .findFirst()
                        .ifPresent(encontrados::add);
            }
            if (!encontrados.isEmpty() && encontrados.size() == operaciones.size()) {
                operaciones.forEach(op -> op.getParameters().removeIf(p -> "path".equals(p.getIn()) && nombre.equals(p.getName())));
                ruta.addParametersItem(encontrados.get(0));
            }
        }
    }

    /** En una solicitud, lo que no es obligatorio acepta {@code null} (el contrato lo marca {@code nullable}). */
    @SuppressWarnings("rawtypes")
    private void marcarOpcionalesComoNulos(Schema<?> esquema, java.util.Map<String, Schema> todos) {
        if (esquema.getProperties() == null) {
            return;
        }
        List<String> requeridas = esquema.getRequired() == null ? List.of() : esquema.getRequired();
        esquema.getProperties().forEach((nombre, propiedad) -> {
            if (requeridas.contains(nombre)) {
                return;
            }
            if (propiedad.get$ref() != null) {
                // Con $ref, `nullable` no se lee junto a la referencia: va en el esquema referenciado (como en el contrato)
                Schema<?> referido = todos.get(propiedad.get$ref().substring(propiedad.get$ref().lastIndexOf('/') + 1));
                if (referido != null) {
                    referido.setNullable(true);
                }
            } else if (propiedad.getNullable() == null) {
                propiedad.setNullable(true);
            }
        });
    }

    /** Quita lo que Java agrega sin que el contrato lo declare: {@code int32}, {@code minLength: 1} y longitudes de correos y URL. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void ajustar(Schema esquema, Set<Schema> visitados) {
        if (esquema == null || !visitados.add(esquema)) {
            return;
        }
        if ("integer".equals(esquema.getType()) && "int32".equals(esquema.getFormat())) {
            esquema.setFormat(null);
        }
        boolean conPatron = esquema.getPattern() != null || "email".equals(esquema.getFormat());
        if (conPatron && Integer.valueOf(1).equals(esquema.getMinLength())) {
            esquema.setMinLength(null);
        }
        if ("email".equals(esquema.getFormat())
                || (esquema.getPattern() != null && esquema.getPattern().startsWith("^https://.*"))) {
            esquema.setMaxLength(null);
        }
        if (esquema.getProperties() != null) {
            ((java.util.Map<String, Schema>) esquema.getProperties()).values().forEach(p -> ajustar(p, visitados));
        }
        if (esquema.getItems() != null) {
            ajustar(esquema.getItems(), visitados);
        }
    }
}
