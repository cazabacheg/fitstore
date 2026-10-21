package com.fitstore.delivery.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.delivery.dto.TarifaDto;
import com.fitstore.delivery.dto.TarifaRequest;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.NivelTarifa;
import com.fitstore.delivery.service.TarifaService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tarifas de envío: solo el ADMIN (CU-28, CA-6-12). */
@RestController
@RequestMapping(value = "/api/v1/admin/tarifas", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "tarifas")
public class AdminTarifaController {

    private final TarifaService tarifas;

    public AdminTarifaController(TarifaService tarifas) {
        this.tarifas = tarifas;
    }

    @GetMapping
    @Operation(operationId = "listarTarifas", summary = "Tarifas de envío")
    public ApiResponse<List<TarifaDto>> listar(@RequestParam(required = false) Modalidad modalidad,
            @RequestParam(required = false) NivelTarifa nivel) {
        return ApiResponse.ok(tarifas.listar(modalidad, nivel));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "crearTarifa", summary = "Crear una tarifa")
    public ApiResponse<TarifaDto> crear(@Valid @RequestBody TarifaRequest pedido) {
        return ApiResponse.ok(tarifas.crear(pedido));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "editarTarifa", summary = "Editar una tarifa (costo, plazo, activa)")
    public ApiResponse<TarifaDto> editar(@PathVariable Long id, @Valid @RequestBody TarifaRequest pedido) {
        return ApiResponse.ok(tarifas.editar(id, pedido));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "eliminarTarifa", summary = "Eliminar una tarifa")
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        tarifas.eliminar(id);
        return ApiResponse.ok("Tarifa eliminada");
    }
}
