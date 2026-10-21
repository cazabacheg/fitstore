package com.fitstore.delivery.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.delivery.dto.AgenciaDto;
import com.fitstore.delivery.dto.CotizacionEnvioDto;
import com.fitstore.delivery.dto.CotizarEnvioRequest;
import com.fitstore.delivery.dto.OpcionEnvioDto;
import com.fitstore.delivery.service.AgenciaService;
import com.fitstore.delivery.service.CotizacionEnvioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Opciones, cotización y agencias activas para el checkout: públicos y de solo lectura (CA-6-01 a CA-6-06). */
@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "envios")
public class EnvioController {

    private final CotizacionEnvioService cotizacion;
    private final AgenciaService agencias;

    public EnvioController(CotizacionEnvioService cotizacion, AgenciaService agencias) {
        this.cotizacion = cotizacion;
        this.agencias = agencias;
    }

    @GetMapping("/envios/opciones")
    @Operation(operationId = "obtenerOpcionesEnvio", summary = "Modalidades con tarifa para un distrito")
    public ApiResponse<List<OpcionEnvioDto>> opciones(@RequestParam String ubigeo) {
        return ApiResponse.ok(cotizacion.opciones(ubigeo));
    }

    @PostMapping("/envios/cotizar")
    @Operation(operationId = "cotizarEnvio", summary = "Cotizar un envío")
    public ApiResponse<CotizacionEnvioDto> cotizar(@Valid @RequestBody CotizarEnvioRequest pedido) {
        return ApiResponse.ok(cotizacion.cotizar(pedido.modalidad(), pedido.ubigeo(), pedido.agenciaId()));
    }

    @GetMapping("/agencias")
    @Operation(operationId = "listarAgenciasActivas", summary = "Agencias activas (para el checkout)")
    public ApiResponse<List<AgenciaDto>> agenciasActivas() {
        return ApiResponse.ok(agencias.listarActivas());
    }
}
