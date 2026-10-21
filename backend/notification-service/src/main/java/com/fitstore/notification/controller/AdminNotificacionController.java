package com.fitstore.notification.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.notification.dto.EnvioDto;
import com.fitstore.notification.dto.PaginaEnviosDto;
import com.fitstore.notification.model.EstadoEnvio;
import com.fitstore.notification.plantillas.CatalogoPlantillas;
import com.fitstore.notification.service.EnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Registro de envíos, detalle y reenvío (P-7-01): solo ADMIN. */
@RestController
@RequestMapping(value = "/api/v1/admin/notificaciones", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "registro")
@SecurityRequirement(name = "bearerAuth")
public class AdminNotificacionController {

    private final EnvioService envios;

    public AdminNotificacionController(EnvioService envios) {
        this.envios = envios;
    }

    @GetMapping
    @Operation(operationId = "listarEnvios", summary = "Registro de envíos")
    public ApiResponse<PaginaEnviosDto> listar(
            @RequestParam(required = false) EstadoEnvio estado,
            @RequestParam(required = false) CatalogoPlantillas plantilla,
            @RequestParam(required = false) String destinatario,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Siempre del más reciente al más antiguo") @RequestParam(required = false) String sort) {
        return ApiResponse.ok(envios.listar(estado, plantilla, destinatario, referencia, desde, hasta, page, size));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "obtenerEnvio", summary = "Detalle de un envío")
    public ApiResponse<EnvioDto> obtener(@PathVariable UUID id) {
        return ApiResponse.ok(envios.obtener(id));
    }

    @PostMapping("/{id}/reenviar")
    @Operation(operationId = "reenviarEnvio", summary = "Reenviar un correo")
    public ApiResponse<EnvioDto> reenviar(@PathVariable UUID id) {
        return ApiResponse.ok(envios.reenviar(id));
    }
}
