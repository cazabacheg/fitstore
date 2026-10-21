package com.fitstore.settings.configuracion.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.settings.configuracion.dto.ConfiguracionPublicaDto;
import com.fitstore.settings.configuracion.service.ConfiguracionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Configuración pública que ambos frontends cargan al iniciar (white-label, RNF-17). */
@RestController
@RequestMapping(value = "/api/v1/settings", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "configuracion-publica")
public class SettingsPublicoController {

    private final ConfiguracionService configuracion;

    public SettingsPublicoController(ConfiguracionService configuracion) {
        this.configuracion = configuracion;
    }

    @GetMapping("/public")
    @Operation(operationId = "obtenerConfiguracionPublica", summary = "Configuración pública de la tienda")
    public ApiResponse<ConfiguracionPublicaDto> publica() {
        return ApiResponse.ok(configuracion.obtenerPublica());
    }
}
