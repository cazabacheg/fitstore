package com.fitstore.delivery.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.delivery.dto.RepartidorDto;
import com.fitstore.delivery.dto.RepartidorRequest;
import com.fitstore.delivery.service.RepartidorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Repartidores de la tienda: OPERADOR y ADMIN (CU-27). */
@RestController
@RequestMapping(value = "/api/v1/admin/repartidores", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('OPERADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "repartidores")
public class AdminRepartidorController {

    private final RepartidorService repartidores;

    public AdminRepartidorController(RepartidorService repartidores) {
        this.repartidores = repartidores;
    }

    @GetMapping
    @Operation(operationId = "listarRepartidores", summary = "Repartidores")
    public ApiResponse<List<RepartidorDto>> listar(@RequestParam(required = false) Boolean activo) {
        return ApiResponse.ok(repartidores.listar(activo));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "crearRepartidor", summary = "Registrar un repartidor")
    public ApiResponse<RepartidorDto> crear(@Valid @RequestBody RepartidorRequest pedido) {
        return ApiResponse.ok(repartidores.crear(pedido));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "editarRepartidor", summary = "Editar un repartidor")
    public ApiResponse<RepartidorDto> editar(@PathVariable Long id, @Valid @RequestBody RepartidorRequest pedido) {
        return ApiResponse.ok(repartidores.editar(id, pedido));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "eliminarRepartidor", summary = "Eliminar un repartidor")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        repartidores.eliminar(id);
        return ApiResponse.ok("Repartidor eliminado");
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(operationId = "desactivarRepartidor", summary = "Desactivar un repartidor")
    public ApiResponse<RepartidorDto> desactivar(@PathVariable Long id) {
        return ApiResponse.ok(repartidores.cambiarActivo(id, false));
    }

    @PatchMapping("/{id}/activar")
    @Operation(operationId = "activarRepartidor", summary = "Activar un repartidor")
    public ApiResponse<RepartidorDto> activar(@PathVariable Long id) {
        return ApiResponse.ok(repartidores.cambiarActivo(id, true));
    }
}
