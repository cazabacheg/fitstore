package com.fitstore.catalog.inventario.controller;

import com.fitstore.catalog.inventario.dto.ReservaDto;
import com.fitstore.catalog.inventario.dto.ReservaRequest;
import com.fitstore.catalog.inventario.service.ReservaService;
import com.fitstore.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reserva de stock que llama order-service al crear un pedido (micro 4.6, paso T1). El gateway no enruta
 * {@code /internal/**} (CA-2-54); aquí además exige un JWT válido.
 */
@RestController
@RequestMapping(path = "/internal/v1/inventario", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReservaInternaController {

    private final ReservaService servicio;

    public ReservaInternaController(ReservaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/reservas")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Reserva creada.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "La reserva del pedido ya existía.")
    public ResponseEntity<ApiResponse<ReservaDto>> reservarStock(@Valid @RequestBody ReservaRequest peticion) {
        ReservaService.Resultado resultado = servicio.reservar(peticion);
        HttpStatus estado = resultado.creada() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(estado).body(ApiResponse.ok(resultado.reserva()));
    }
}
