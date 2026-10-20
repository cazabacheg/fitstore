package com.fitstore.settings.reclamaciones.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.security.UsuarioActual;
import com.fitstore.settings.reclamaciones.dto.FiltroReclamos;
import com.fitstore.settings.reclamaciones.dto.ReclamoDto;
import com.fitstore.settings.reclamaciones.dto.ReclamoResumenDto;
import com.fitstore.settings.reclamaciones.dto.ResponderReclamoRequest;
import com.fitstore.settings.reclamaciones.model.EstadoReclamo;
import com.fitstore.settings.reclamaciones.model.TipoReclamo;
import com.fitstore.settings.reclamaciones.service.ReclamoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Atención de reclamos en el backoffice: el OPERADOR consulta, solo el ADMIN toma y responde. */
@RestController
@RequestMapping("/api/v1/admin/reclamos")
@Validated
@Tag(name = "reclamos")
public class AdminReclamoController {

    private final ReclamoService reclamos;

    public AdminReclamoController(ReclamoService reclamos) {
        this.reclamos = reclamos;
    }

    @GetMapping
    @PreAuthorize("hasRole('OPERADOR')")
    @Operation(operationId = "listarReclamos", summary = "Bandeja de reclamos")
    public ApiResponse<PaginaResponse<ReclamoResumenDto>> listar(
            @RequestParam(required = false) EstadoReclamo estado,
            @RequestParam(required = false) TipoReclamo tipo,
            @RequestParam(required = false, defaultValue = "false") boolean porVencer,
            @RequestParam(required = false, defaultValue = "false") boolean vencidos,
            @RequestParam(required = false) @Size(max = 60) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<String> sort) {
        FiltroReclamos filtro = new FiltroReclamos(estado, tipo, porVencer, vencidos, q, desde, hasta);
        return ApiResponse.ok(reclamos.listar(filtro, PageRequest.of(page, size, orden(sort))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OPERADOR')")
    @Operation(operationId = "obtenerReclamo", summary = "Detalle de un reclamo")
    public ApiResponse<ReclamoDto> obtener(@PathVariable Long id) {
        return ApiResponse.ok(reclamos.obtener(id));
    }

    @PatchMapping("/{id}/en-atencion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "tomarReclamo", summary = "Marcar en atención")
    public ApiResponse<ReclamoDto> tomar(@PathVariable Long id) {
        return ApiResponse.ok("El reclamo quedó en atención", reclamos.tomar(id));
    }

    @PostMapping("/{id}/respuesta")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "responderReclamo", summary = "Responder un reclamo")
    public ApiResponse<ReclamoDto> responder(@PathVariable Long id,
            @Valid @RequestBody ResponderReclamoRequest solicitud, Authentication autenticacion) {
        return ApiResponse.ok("Registramos la respuesta",
                reclamos.responder(id, solicitud.respuesta(), UsuarioActual.de(autenticacion).id()));
    }

    /** {@code campo,asc|desc}; sin orden se usa el más reciente primero. */
    private static Sort orden(List<String> criterios) {
        if (criterios == null || criterios.isEmpty()) {
            return Sort.unsorted();
        }
        List<Sort.Order> ordenes = new ArrayList<>();
        for (String criterio : criterios) {
            String[] partes = criterio.split(",");
            Sort.Direction direccion = partes.length > 1 && "desc".equalsIgnoreCase(partes[1])
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            ordenes.add(new Sort.Order(direccion, partes[0]));
        }
        return Sort.by(ordenes);
    }
}
