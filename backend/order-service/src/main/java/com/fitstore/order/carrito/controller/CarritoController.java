package com.fitstore.order.carrito.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.order.carrito.dto.AgregarItemRequest;
import com.fitstore.order.carrito.dto.CambiarCantidadRequest;
import com.fitstore.order.carrito.dto.CarritoDto;
import com.fitstore.order.carrito.model.CarritoId;
import com.fitstore.order.carrito.service.CarritoService;
import com.fitstore.order.pedidos.controller.ExtensionDeAlcance;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carrito de un visitante (cabecera {@code X-Carrito-Id}) o de un cliente con sesión (JWT). Con sesión de cliente se
 * ignora la cabecera: el carrito del cliente y el anónimo son independientes (extensión de alcance, HU-4-02).
 */
@RestController
@RequestMapping(path = "/api/v1/carrito", produces = MediaType.APPLICATION_JSON_VALUE)
public class CarritoController {

    static final String CABECERA = "X-Carrito-Id";

    private final CarritoService servicio;

    public CarritoController(CarritoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public ApiResponse<CarritoDto> obtenerCarrito(Authentication autenticacion,
            @RequestHeader(name = CABECERA, required = false) String carritoId) {
        CarritoId carrito = identificar(autenticacion, carritoId, false);
        return ApiResponse.ok(carrito == null ? CarritoDto.vacio() : servicio.ver(carrito));
    }

    @DeleteMapping
    public ApiResponse<CarritoDto> vaciarCarrito(Authentication autenticacion,
            @RequestHeader(name = CABECERA, required = false) String carritoId) {
        CarritoId carrito = identificar(autenticacion, carritoId, false);
        return ApiResponse.ok(carrito == null ? CarritoDto.vacio() : servicio.vaciar(carrito));
    }

    @PostMapping(path = "/items", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CarritoDto> agregarAlCarrito(Authentication autenticacion,
            @RequestHeader(name = CABECERA, required = false) String carritoId,
            @Valid @RequestBody AgregarItemRequest peticion) {
        CarritoId carrito = identificar(autenticacion, carritoId, true);
        return ApiResponse.ok(servicio.agregar(carrito, peticion.varianteId(), peticion.cantidad()));
    }

    @PutMapping(path = "/items/{varianteId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<CarritoDto> cambiarCantidadCarrito(Authentication autenticacion,
            @RequestHeader(name = CABECERA, required = false) String carritoId, @PathVariable long varianteId,
            @Valid @RequestBody CambiarCantidadRequest peticion) {
        CarritoId carrito = identificar(autenticacion, carritoId, true);
        return ApiResponse.ok(servicio.cambiarCantidad(carrito, varianteId, peticion.cantidad()));
    }

    @DeleteMapping("/items/{varianteId}")
    public ApiResponse<CarritoDto> quitarDelCarrito(Authentication autenticacion,
            @RequestHeader(name = CABECERA, required = false) String carritoId, @PathVariable long varianteId) {
        CarritoId carrito = identificar(autenticacion, carritoId, true);
        return ApiResponse.ok(servicio.quitar(carrito, varianteId));
    }

    /** Extensión de alcance (HU-4-02): conserva la firma del contrato, sin lógica. */
    @PostMapping("/fusionar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ApiResponse<CarritoDto> fusionarCarrito(
            @RequestHeader(name = CABECERA, required = false) String carritoId) {
        throw ExtensionDeAlcance.de("Fusionar el carrito");
    }

    /**
     * Un cliente con sesión usa su carrito; si no, el anónimo de la cabecera. Sin ninguno de los dos: {@code null}
     * cuando solo se consulta, y 400 cuando se quiere modificar.
     */
    static CarritoId identificar(Authentication autenticacion, String carritoId, boolean obligatorio) {
        if (autenticacion instanceof JwtAuthenticationToken jwt
                && "CLIENTE".equals(jwt.getToken().getClaimAsString("tipo"))) {
            return CarritoId.cliente(UUID.fromString(jwt.getToken().getSubject()));
        }
        if (carritoId == null || carritoId.isBlank()) {
            if (obligatorio) {
                throw sinCarrito();
            }
            return null;
        }
        try {
            return CarritoId.anonimo(UUID.fromString(carritoId).toString());
        } catch (IllegalArgumentException e) {
            throw sinCarrito();
        }
    }

    private static FitstoreException sinCarrito() {
        return new FitstoreException(HttpStatus.BAD_REQUEST, "CARRITO_NO_IDENTIFICADO",
                "Falta el identificador del carrito (X-Carrito-Id) o la sesión");
    }
}
