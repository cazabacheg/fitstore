package com.fitstore.delivery.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.delivery.dto.AgenciaDto;
import com.fitstore.delivery.dto.AgenciaRequest;
import com.fitstore.delivery.dto.OrdenRequest;
import com.fitstore.delivery.service.AgenciaService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Agencias de transporte: solo el ADMIN (CU-28). */
@RestController
@RequestMapping(value = "/api/v1/admin/agencias", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "agencias-admin")
public class AdminAgenciaController {

    private final AgenciaService agencias;

    public AdminAgenciaController(AgenciaService agencias) {
        this.agencias = agencias;
    }

    @GetMapping
    @Operation(operationId = "listarAgenciasAdmin", summary = "Todas las agencias")
    public ApiResponse<List<AgenciaDto>> listar() {
        return ApiResponse.ok(agencias.listarTodas());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "crearAgencia", summary = "Crear una agencia")
    public ApiResponse<AgenciaDto> crear(@Valid @RequestBody AgenciaRequest pedido) {
        return ApiResponse.ok(agencias.crear(pedido));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "editarAgencia", summary = "Editar una agencia")
    public ApiResponse<AgenciaDto> editar(@PathVariable Long id, @Valid @RequestBody AgenciaRequest pedido) {
        return ApiResponse.ok(agencias.editar(id, pedido));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "eliminarAgencia", summary = "Eliminar una agencia")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        agencias.eliminar(id);
        return ApiResponse.ok("Agencia eliminada");
    }

    @PatchMapping("/{id}/desactivar")
    @Operation(operationId = "desactivarAgencia", summary = "Desactivar una agencia")
    public ApiResponse<AgenciaDto> desactivar(@PathVariable Long id) {
        return ApiResponse.ok(agencias.cambiarActiva(id, false));
    }

    @PatchMapping("/{id}/activar")
    @Operation(operationId = "activarAgencia", summary = "Activar una agencia")
    public ApiResponse<AgenciaDto> activar(@PathVariable Long id) {
        return ApiResponse.ok(agencias.cambiarActiva(id, true));
    }

    @PatchMapping("/{id}/orden")
    @Operation(operationId = "ordenarAgencia", summary = "Mover una agencia en el orden del checkout")
    public ApiResponse<List<AgenciaDto>> ordenar(@PathVariable Long id, @Valid @RequestBody OrdenRequest pedido) {
        return ApiResponse.ok(agencias.ordenar(id, pedido.orden()));
    }
}
