package com.fitstore.auth.controller;

import com.fitstore.auth.dto.response.ClienteDetalleResponse;
import com.fitstore.auth.dto.response.ClienteResumenResponse;
import com.fitstore.auth.security.SoloAdmin;
import com.fitstore.auth.security.SoloStaff;
import com.fitstore.auth.service.ClienteService;
import com.fitstore.auth.service.Paginacion;
import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.PaginaResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Backoffice: consulta de clientes (OPERADOR y ADMIN) y bloqueo por fraude o abuso (ADMIN). */
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteAdminController {

    private static final Set<String> ORDENABLES = Set.of("nombres", "apellidos", "email", "dni", "activo", "creadoEn");

    private final ClienteService clientes;

    public ClienteAdminController(ClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    @SoloStaff
    @Operation(operationId = "listarClientes", summary = "Buscar clientes")
    public ApiResponse<PaginaResponse<ClienteResumenResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) List<String> sort) {
        var pagina = Paginacion.de(page, size, sort, ORDENABLES, Sort.by(Sort.Direction.DESC, "creadoEn"));
        return ApiResponse.ok(clientes.buscar(q, activo, pagina));
    }

    @GetMapping("/{id}")
    @SoloStaff
    @Operation(operationId = "obtenerCliente", summary = "Ver un cliente con sus direcciones")
    public ApiResponse<ClienteDetalleResponse> detalle(@PathVariable UUID id) {
        return ApiResponse.ok(clientes.detalle(id));
    }

    @PatchMapping("/{id}/desactivar")
    @SoloAdmin
    @Operation(operationId = "bloquearCliente", summary = "Bloquear a un cliente")
    public ApiResponse<ClienteDetalleResponse> bloquear(@PathVariable UUID id) {
        return ApiResponse.ok("Cliente bloqueado", clientes.bloquear(id));
    }

    @PatchMapping("/{id}/activar")
    @SoloAdmin
    @Operation(operationId = "desbloquearCliente", summary = "Desbloquear a un cliente")
    public ApiResponse<ClienteDetalleResponse> desbloquear(@PathVariable UUID id) {
        return ApiResponse.ok("Cliente desbloqueado", clientes.desbloquear(id));
    }
}
