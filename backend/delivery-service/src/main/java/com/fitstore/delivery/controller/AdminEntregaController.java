package com.fitstore.delivery.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.delivery.config.ExtensionDeAlcanceException;
import com.fitstore.delivery.dto.AsignarRequest;
import com.fitstore.delivery.dto.DevueltaRequest;
import com.fitstore.delivery.dto.EntregaDto;
import com.fitstore.delivery.dto.EntregasDiaDto;
import com.fitstore.delivery.dto.EnvioAgenciaRequest;
import com.fitstore.delivery.dto.FiltroEntregas;
import com.fitstore.delivery.dto.ObservacionRequest;
import com.fitstore.delivery.dto.PaginaEntregasDto;
import com.fitstore.delivery.dto.ResumenEntregasDto;
import com.fitstore.delivery.dto.VersionRequest;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.service.Actor;
import com.fitstore.delivery.service.EntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Entregas del backoffice: las gestionan OPERADOR y ADMIN (CU-26 y CU-26b). */
@RestController
@RequestMapping(value = "/api/v1/admin/entregas", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('OPERADOR')")
@Validated
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "entregas")
public class AdminEntregaController {

    private static final Set<String> ORDENABLES = Set.of("creadoEn", "fechaProgramada", "codigoPedido", "estado");

    private final EntregaService entregas;

    public AdminEntregaController(EntregaService entregas) {
        this.entregas = entregas;
    }

    @GetMapping
    @Operation(operationId = "listarEntregas", summary = "Entregas con filtros",
            description = "Las pendientes primero; contadores por estado para las pestañas (CA-6-31, 32).")
    public ApiResponse<PaginaEntregasDto> listar(@RequestParam(required = false) Modalidad modalidad,
            @RequestParam(required = false) EstadoEntrega estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long repartidorId,
            @RequestParam(required = false) String codigo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<String> sort) {
        FiltroEntregas filtro = new FiltroEntregas(modalidad, estado, fecha, repartidorId, codigo);
        return ApiResponse.ok(entregas.listar(filtro, PageRequest.of(page, size, orden(sort))));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "obtenerEntrega", summary = "Detalle de una entrega")
    public ApiResponse<EntregaDto> obtener(@PathVariable UUID id) {
        return ApiResponse.ok(entregas.obtener(id));
    }

    @PatchMapping("/{id}/asignar")
    @Operation(operationId = "asignarEntrega", summary = "Asignar o reprogramar")
    public ApiResponse<EntregaDto> asignar(@PathVariable UUID id, @Valid @RequestBody AsignarRequest pedido,
            Authentication sesion) {
        return ApiResponse.ok(entregas.asignar(id, pedido, Actor.de(sesion)));
    }

    @PatchMapping("/{id}/en-ruta")
    @Operation(operationId = "marcarEnRuta", summary = "Sale a repartir")
    public ApiResponse<EntregaDto> enRuta(@PathVariable UUID id, @Valid @RequestBody VersionRequest pedido,
            Authentication sesion) {
        return ApiResponse.ok(entregas.enRuta(id, pedido, Actor.de(sesion)));
    }

    @PatchMapping("/{id}/entregada")
    @Operation(operationId = "marcarEntregada", summary = "Entregada (local) o recogida en la agencia")
    public ApiResponse<EntregaDto> entregada(@PathVariable UUID id, @Valid @RequestBody VersionRequest pedido,
            Authentication sesion) {
        return ApiResponse.ok(entregas.entregada(id, pedido, Actor.de(sesion)));
    }

    @PatchMapping("/{id}/fallida")
    @Operation(operationId = "marcarFallida", summary = "No se pudo entregar")
    public ApiResponse<EntregaDto> fallida(@PathVariable UUID id, @Valid @RequestBody ObservacionRequest pedido,
            Authentication sesion) {
        return ApiResponse.ok(entregas.fallida(id, pedido, Actor.de(sesion)));
    }

    @PatchMapping("/{id}/enviada-agencia")
    @Operation(operationId = "registrarEnvioAgencia", summary = "Registrar el envío por agencia")
    public ApiResponse<EntregaDto> enviadaAgencia(@PathVariable UUID id, @Valid @RequestBody EnvioAgenciaRequest pedido,
            Authentication sesion) {
        return ApiResponse.ok(entregas.enviadaAgencia(id, pedido, Actor.de(sesion)));
    }

    @PatchMapping("/{id}/disponible-agencia")
    @Operation(operationId = "marcarDisponibleAgencia", summary = "Llegó a la agencia de destino")
    public ApiResponse<EntregaDto> disponibleAgencia(@PathVariable UUID id, @Valid @RequestBody VersionRequest pedido,
            Authentication sesion) {
        return ApiResponse.ok(entregas.disponibleAgencia(id, pedido, Actor.de(sesion)));
    }

    // --- Extensión de alcance: están en el contrato, pero no se construyen (extension-alcance.md) ---

    /**
     * Entregas del día y hoja de ruta (HU-6-08, CA-6-28 y 29): extensión de alcance, ver
     * {@code docs/implementacion/extension-alcance.md}. Responde 501 sin lógica.
     */
    @GetMapping("/dia")
    @Operation(operationId = "obtenerEntregasDia", summary = "Entregas del día y hoja de ruta")
    public ApiResponse<EntregasDiaDto> entregasDelDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long repartidorId) {
        throw new ExtensionDeAlcanceException();
    }

    /**
     * Resumen de entregas por estado para el dashboard (CA-6-30): extensión de alcance, ver
     * {@code docs/implementacion/extension-alcance.md}. Responde 501 sin lógica.
     */
    @GetMapping("/resumen")
    @Operation(operationId = "obtenerResumenEntregas", summary = "Entregas de un día por estado (dashboard)")
    public ApiResponse<ResumenEntregasDto> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        throw new ExtensionDeAlcanceException();
    }

    /**
     * Devolución a la tienda (HU-6-10, CA-6-33 y 34): extensión de alcance, ver
     * {@code docs/implementacion/extension-alcance.md}. Responde 501 sin lógica.
     */
    @PatchMapping("/{id}/devuelta")
    @Operation(operationId = "marcarDevuelta", summary = "Devuelto a tienda")
    public ApiResponse<EntregaDto> devuelta(@PathVariable UUID id, @Valid @RequestBody DevueltaRequest pedido) {
        throw new ExtensionDeAlcanceException();
    }

    /** Ordena por los campos permitidos ({@code campo,asc|desc}); lo demás se ignora. */
    private static Sort orden(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> ordenes = new ArrayList<>();
        for (String criterio : sort) {
            String[] partes = criterio.split(",");
            if (ORDENABLES.contains(partes[0])) {
                boolean descendente = partes.length > 1 && "desc".equalsIgnoreCase(partes[1]);
                ordenes.add(descendente ? Sort.Order.desc(partes[0]) : Sort.Order.asc(partes[0]));
            }
        }
        return ordenes.isEmpty() ? Sort.unsorted() : Sort.by(ordenes);
    }
}
