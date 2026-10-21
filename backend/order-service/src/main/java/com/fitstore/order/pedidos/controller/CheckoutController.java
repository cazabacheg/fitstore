package com.fitstore.order.pedidos.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.security.UsuarioActual;
import com.fitstore.order.pedidos.dto.ConfirmarPedidoRequest;
import com.fitstore.order.pedidos.dto.EntregaElegidaRequest;
import com.fitstore.order.pedidos.dto.PedidoCreadoDto;
import com.fitstore.order.pedidos.dto.ResumenCheckoutDto;
import com.fitstore.order.pedidos.service.CheckoutService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Resumen y confirmación del pedido del cliente (HU-4-03 y HU-4-04). El cliente sale siempre del token. */
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class CheckoutController {

    private final CheckoutService servicio;

    public CheckoutController(CheckoutService servicio) {
        this.servicio = servicio;
    }

    @PostMapping(path = "/checkout/resumen", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CLIENTE')")
    public ApiResponse<ResumenCheckoutDto> resumirCheckout(Authentication autenticacion,
            @Valid @RequestBody EntregaElegidaRequest peticion) {
        return ApiResponse.ok(servicio.resumir(clienteId(autenticacion), peticion));
    }

    @PostMapping(path = "/pedidos", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('CLIENTE')")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pedido creado.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "El pedido de una confirmación anterior con esa clave.")
    public ResponseEntity<ApiResponse<PedidoCreadoDto>> confirmarPedido(Authentication autenticacion,
            @RequestHeader(name = "Idempotency-Key", required = false) String claveIdempotencia,
            @Valid @RequestBody ConfirmarPedidoRequest peticion) {
        if (claveIdempotencia == null || claveIdempotencia.isBlank() || claveIdempotencia.length() > 100) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION",
                    "Falta la cabecera Idempotency-Key (hasta 100 caracteres)");
        }
        CheckoutService.Confirmacion confirmacion = servicio.confirmar(clienteId(autenticacion), peticion,
                claveIdempotencia);
        return ResponseEntity.status(confirmacion.nuevo() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.ok(confirmacion.nuevo() ? "Pedido creado" : "Operación exitosa", confirmacion.pedido()));
    }

    static UUID clienteId(Authentication autenticacion) {
        return UUID.fromString(UsuarioActual.de(autenticacion).id());
    }
}
