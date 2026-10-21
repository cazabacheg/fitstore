package com.fitstore.catalog.inventario.controller;

import com.fitstore.catalog.inventario.dto.AjusteRequest;
import com.fitstore.catalog.inventario.dto.ConfiguracionInventarioDto;
import com.fitstore.catalog.inventario.dto.EntradaRequest;
import com.fitstore.catalog.inventario.dto.KardexDto;
import com.fitstore.catalog.inventario.dto.MinimoRequest;
import com.fitstore.catalog.inventario.dto.StockDto;
import com.fitstore.catalog.inventario.service.InventarioService;
import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.security.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Inventario del backoffice (micro 4.1): OPERADOR y ADMIN; la configuración de la tienda es solo del ADMIN. */
@RestController
@RequestMapping(path = "/api/v1/admin/inventario", produces = MediaType.APPLICATION_JSON_VALUE)
public class InventarioController {

    private final InventarioService servicio;

    public InventarioController(InventarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasRole('OPERADOR')")
    public ApiResponse<PaginaResponse<StockDto>> listarStock(
            @RequestParam(required = false) @Size(max = 80) String q,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Boolean soloStockBajo,
            @RequestParam(required = false) Boolean soloAgotados,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<String> sort) {
        return ApiResponse.ok(servicio.listar(q, productoId, Boolean.TRUE.equals(soloStockBajo),
                Boolean.TRUE.equals(soloAgotados), page, size, sort));
    }

    @GetMapping("/{varianteId}/movimientos")
    @PreAuthorize("hasRole('OPERADOR')")
    public ApiResponse<KardexDto> obtenerKardex(@PathVariable Long varianteId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(servicio.kardex(varianteId, PageRequest.of(page, size)));
    }

    @PostMapping("/entradas")
    @PreAuthorize("hasRole('OPERADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<StockDto>> registrarEntrada(@Valid @RequestBody EntradaRequest peticion, Authentication sesion) {
        return ApiResponse.ok(servicio.registrarEntrada(peticion, usuario(sesion)));
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasRole('OPERADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StockDto> registrarAjuste(@Valid @RequestBody AjusteRequest peticion, Authentication sesion) {
        return ApiResponse.ok(servicio.registrarAjuste(peticion, usuario(sesion)));
    }

    @PatchMapping("/{varianteId}/minimo")
    @PreAuthorize("hasRole('OPERADOR')")
    public ApiResponse<StockDto> cambiarMinimoVariante(@PathVariable Long varianteId, @Valid @RequestBody MinimoRequest peticion) {
        return ApiResponse.ok(servicio.cambiarMinimo(varianteId, peticion.minimo()));
    }

    @GetMapping("/configuracion")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ConfiguracionInventarioDto> obtenerConfiguracionInventario() {
        return ApiResponse.ok(servicio.obtenerConfiguracion());
    }

    @PutMapping("/configuracion")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ConfiguracionInventarioDto> guardarConfiguracionInventario(
            @Valid @RequestBody ConfiguracionInventarioDto peticion) {
        return ApiResponse.ok(servicio.guardarConfiguracion(peticion));
    }

    private static UUID usuario(Authentication sesion) {
        return UUID.fromString(UsuarioActual.de(sesion).id());
    }
}
