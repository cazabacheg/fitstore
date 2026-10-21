package com.fitstore.order.pedidos.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.security.UsuarioActual;
import com.fitstore.order.pedidos.dto.PedidoDetalleDto;
import com.fitstore.order.pedidos.dto.PedidoResumenDto;
import com.fitstore.order.pedidos.service.PedidoConsultaService;
import com.fitstore.order.pedidos.service.PedidoService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Mis pedidos, su seguimiento y su cancelación (HU-4-06 y HU-4-07). El cliente sale siempre del token. */
@RestController
@Validated
@RequestMapping(path = "/api/v1/pedidos", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('CLIENTE')")
public class PedidoController {

    private final PedidoConsultaService consultas;
    private final PedidoService pedidos;

    public PedidoController(PedidoConsultaService consultas, PedidoService pedidos) {
        this.consultas = consultas;
        this.pedidos = pedidos;
    }

    @GetMapping
    public ApiResponse<PaginaResponse<PedidoResumenDto>> listarMisPedidos(Authentication autenticacion,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(consultas.misPedidos(clienteId(autenticacion), page, size));
    }

    @GetMapping("/{codigo}")
    public ApiResponse<PedidoDetalleDto> obtenerMiPedido(Authentication autenticacion, @PathVariable String codigo) {
        return ApiResponse.ok(consultas.detalle(clienteId(autenticacion), codigo));
    }

    @PatchMapping("/{codigo}/cancelar")
    public ApiResponse<PedidoDetalleDto> cancelarMiPedido(Authentication autenticacion, @PathVariable String codigo) {
        UUID cliente = clienteId(autenticacion);
        pedidos.cancelarComoCliente(cliente, codigo);
        return ApiResponse.ok("Pedido cancelado", consultas.detalle(cliente, codigo));
    }

    private static UUID clienteId(Authentication autenticacion) {
        return UUID.fromString(UsuarioActual.de(autenticacion).id());
    }
}
