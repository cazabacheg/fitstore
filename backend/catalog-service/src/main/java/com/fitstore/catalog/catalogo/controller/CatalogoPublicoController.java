package com.fitstore.catalog.catalogo.controller;

import com.fitstore.catalog.catalogo.dto.CategoriaNodoDto;
import com.fitstore.catalog.catalogo.dto.ColorDto;
import com.fitstore.catalog.catalogo.dto.GuiaTallasDto;
import com.fitstore.catalog.catalogo.dto.ProductoDetalleDto;
import com.fitstore.catalog.catalogo.dto.ProductoTarjetaDto;
import com.fitstore.catalog.catalogo.dto.TallaDto;
import com.fitstore.catalog.catalogo.service.CatalogoPublicoService;
import com.fitstore.catalog.catalogo.service.FiltroProductos;
import com.fitstore.catalog.catalogo.service.OrdenProductos;
import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.PaginaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo público de la tienda (micro 4.1, SPEC-2 HU-2-01 y HU-2-02): sin sesión y solo lectura. */
@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogoPublicoController {

    private final CatalogoPublicoService servicio;

    public CatalogoPublicoController(CatalogoPublicoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/categorias")
    public ApiResponse<List<CategoriaNodoDto>> obtenerArbolCategorias() {
        return ApiResponse.ok(servicio.arbolCategorias());
    }

    @GetMapping("/categorias/{slug}/guia-tallas")
    public ApiResponse<GuiaTallasDto> obtenerGuiaTallas(@PathVariable String slug) {
        return ApiResponse.ok(servicio.guiaTallas(slug));
    }

    @GetMapping("/productos")
    public ApiResponse<PaginaResponse<ProductoTarjetaDto>> listarProductos(
            @RequestParam(required = false) @Size(max = 80) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) List<String> talla,
            @RequestParam(required = false) List<Long> color,
            @RequestParam(required = false) @Min(0) BigDecimal precioMin,
            @RequestParam(required = false) @Min(0) BigDecimal precioMax,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(48) int size,
            @RequestParam(defaultValue = OrdenProductos.NOVEDAD)
            @Schema(allowableValues = {"novedad", "precio,asc", "precio,desc", "nombre"}) String sort) {
        FiltroProductos filtro = new FiltroProductos(q, categoria, talla, color, precioMin, precioMax, Boolean.TRUE.equals(disponible));
        return ApiResponse.ok(servicio.listar(filtro, OrdenProductos.paginacion(page, size, sort)));
    }

    @GetMapping("/productos/destacados")
    public ApiResponse<List<ProductoTarjetaDto>> listarDestacados() {
        return ApiResponse.ok(servicio.destacados());
    }

    @GetMapping("/productos/{slug}")
    public ApiResponse<ProductoDetalleDto> obtenerProducto(@PathVariable String slug) {
        return ApiResponse.ok(servicio.detalle(slug));
    }

    @GetMapping("/tallas")
    public ApiResponse<List<TallaDto>> listarTallas() {
        return ApiResponse.ok(servicio.tallas());
    }

    @GetMapping("/colores")
    public ApiResponse<List<ColorDto>> listarColores() {
        return ApiResponse.ok(servicio.colores());
    }
}
