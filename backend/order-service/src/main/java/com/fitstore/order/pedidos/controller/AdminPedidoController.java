package com.fitstore.order.pedidos.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.security.UsuarioActual;
import com.fitstore.order.pedidos.dto.AvanzarEstadoRequest;
import com.fitstore.order.pedidos.dto.CancelarStaffRequest;
import com.fitstore.order.pedidos.dto.PaginaPedidosAdminDto;
import com.fitstore.order.pedidos.dto.PedidoAdminDto;
import com.fitstore.order.pedidos.dto.ReembolsoPostventaRequest;
import com.fitstore.order.pedidos.dto.ResumenVentasDto;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.TipoActor;
import com.fitstore.order.pedidos.service.PedidoConsultaService;
import com.fitstore.order.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pedidos en el backoffice (HU-4-08 a HU-4-10): lista con filtros y contadores, detalle con las transiciones que
 * permite la máquina de estados, avance y cancelación. Operador y administrador (ADMIN hereda de OPERADOR).
 */
@RestController
@Validated
@RequestMapping(path = "/api/v1/admin/pedidos", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminPedidoController {

    private static final Set<String> ORDENABLES = Set.of("creadoEn", "total", "estado", "codigo");

    private final PedidoConsultaService consultas;
    private final PedidoService pedidos;

    public AdminPedidoController(PedidoConsultaService consultas, PedidoService pedidos) {
        this.consultas = consultas;
        this.pedidos = pedidos;
    }

    @GetMapping
    @PreAuthorize("hasRole('OPERADOR')")
    public ApiResponse<PaginaPedidosAdminDto> listarPedidosAdmin(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) Modalidad modalidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) @Pattern(regexp = "^FS-[0-9]{1,6}$") String codigo,
            @RequestParam(required = false) @Size(max = 80) String cliente,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<@Pattern(regexp = "^[a-zA-Z]+(,(asc|desc))?$") String> sort) {
        var filtros = new PedidoConsultaService.Filtros(estado, modalidad, desde, hasta, codigo, cliente, clienteId);
        return ApiResponse.ok(consultas.listarAdmin(filtros, page, size, ordenar(sort)));
    }

    /** Extensión de alcance (HU-4-11): conserva la firma del contrato, sin lógica. */
    @GetMapping("/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ResumenVentasDto> obtenerResumenVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        throw ExtensionDeAlcance.de("El resumen de ventas");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OPERADOR')")
    public ApiResponse<PedidoAdminDto> obtenerPedidoAdmin(@PathVariable UUID id) {
        return ApiResponse.ok(consultas.detalleAdmin(id));
    }

    @PatchMapping(path = "/{id}/estado", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('OPERADOR')")
    public ApiResponse<PedidoAdminDto> avanzarEstadoPedido(Authentication autenticacion, @PathVariable UUID id,
            @Valid @RequestBody AvanzarEstadoRequest peticion) {
        pedidos.avanzar(id, peticion, actor(autenticacion));
        return ApiResponse.ok(consultas.detalleAdmin(id));
    }

    @PatchMapping(path = "/{id}/cancelar", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('OPERADOR')")
    public ApiResponse<PedidoAdminDto> cancelarPedidoStaff(Authentication autenticacion, @PathVariable UUID id,
            @Valid @RequestBody CancelarStaffRequest peticion) {
        pedidos.cancelarComoStaff(id, peticion.version(), peticion.motivo(), actor(autenticacion));
        return ApiResponse.ok(consultas.detalleAdmin(id));
    }

    /** Extensión de alcance (HU-4-12): conserva la firma del contrato, sin lógica. */
    @PostMapping(path = "/{id}/reembolsos", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PedidoAdminDto> registrarReembolsoPostventa(@PathVariable UUID id,
            @Valid @RequestBody ReembolsoPostventaRequest peticion) {
        throw ExtensionDeAlcance.de("El reembolso postventa");
    }

    private static PedidoService.Actor actor(Authentication autenticacion) {
        UsuarioActual usuario = UsuarioActual.de(autenticacion);
        Jwt jwt = (Jwt) autenticacion.getPrincipal();
        return new PedidoService.Actor(TipoActor.STAFF, UUID.fromString(usuario.id()), jwt.getClaimAsString("nombre"));
    }

    /** Solo se ordena por columnas conocidas; sin orden pedido manda la prioridad de preparación. */
    private static Sort ordenar(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> ordenes = sort.stream().map(texto -> texto.split(",")).filter(p -> ORDENABLES.contains(p[0]))
                .map(p -> p.length > 1 && "desc".equalsIgnoreCase(p[1]) ? Sort.Order.desc(p[0]) : Sort.Order.asc(p[0]))
                .toList();
        return ordenes.isEmpty() ? Sort.unsorted() : Sort.by(ordenes);
    }
}
