package com.fitstore.payment.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.payment.dto.PagoDetalle;
import com.fitstore.payment.dto.PagoResumen;
import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.service.AdminPagoService;
import com.fitstore.payment.service.BoletaService;
import com.fitstore.payment.service.FiltroPagos;
import com.fitstore.payment.service.Paginacion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Pagos, intentos y boletas en el backoffice: solo ADMIN (CU-25, CA-5-35). */
@RestController
@RequestMapping(path = "/api/v1/admin/pagos", produces = MediaType.APPLICATION_JSON_VALUE)
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminPagoController {

    private static final Map<String, String> ORDENES = Map.of(
            "creadoEn", "creadoEn", "monto", "monto", "codigoPedido", "codigoPedido", "estado", "estado");
    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Order.desc("creadoEn"));

    private final AdminPagoService pagos;
    private final BoletaService boletas;

    public AdminPagoController(AdminPagoService pagos, BoletaService boletas) {
        this.pagos = pagos;
        this.boletas = boletas;
    }

    @GetMapping
    public ApiResponse<PaginaResponse<PagoResumen>> listarPagos(
            @RequestParam(required = false) EstadoIntencion estado,
            @RequestParam(required = false) Proveedor proveedor,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<String> sort) {
        FiltroPagos filtro = new FiltroPagos(estado, proveedor, codigo, desde, hasta);
        return ApiResponse.ok(PaginaResponse.de(
                pagos.listar(filtro, Paginacion.de(page, size, sort, ORDENES, ORDEN_POR_DEFECTO))));
    }

    @GetMapping("/{id}")
    public ApiResponse<PagoDetalle> obtenerPago(@PathVariable UUID id) {
        return ApiResponse.ok(pagos.obtener(id));
    }

    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Boleta simulada en PDF.",
            content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary")))
    @GetMapping(path = "/{id}/boleta", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> descargarBoletaAdmin(@PathVariable UUID id) {
        return BoletaRespuestas.pdf(boletas.descargarDeLaIntencion(id));
    }
}
