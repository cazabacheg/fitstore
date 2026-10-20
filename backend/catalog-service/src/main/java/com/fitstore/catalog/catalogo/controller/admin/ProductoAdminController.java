package com.fitstore.catalog.catalogo.controller.admin;

import com.fitstore.catalog.catalogo.dto.CategoriasProductoRequest;
import com.fitstore.catalog.catalogo.dto.DescuentoProductoRequest;
import com.fitstore.catalog.catalogo.dto.DestacadoRequest;
import com.fitstore.catalog.catalogo.dto.EditarVarianteRequest;
import com.fitstore.catalog.catalogo.dto.MatrizRequest;
import com.fitstore.catalog.catalogo.dto.ProductoAdminDto;
import com.fitstore.catalog.catalogo.dto.ProductoAdminResumenDto;
import com.fitstore.catalog.catalogo.dto.ProductoRequest;
import com.fitstore.catalog.catalogo.dto.VarianteRequest;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.service.Paginacion;
import com.fitstore.catalog.catalogo.service.ProductoService;
import com.fitstore.common.api.ApiResponse;
import com.fitstore.common.api.PaginaResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Productos y variantes del backoffice (micro 4.1). El OPERADOR solo lee (lista y ficha); el resto es solo ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class ProductoAdminController {

    private static final Map<String, String> ORDENES = Map.of("nombre", "nombre", "codigo", "codigo",
            "precioDesde", "precioDesde", "estado", "estado", "creadoEn", "creadoEn");
    private static final Sort ORDEN_POR_DEFECTO = Sort.by(Sort.Order.desc("creadoEn"), Sort.Order.desc("id"));

    private final ProductoService servicio;

    public ProductoAdminController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/productos")
    @PreAuthorize("hasRole('OPERADOR')")
    public ApiResponse<PaginaResponse<ProductoAdminResumenDto>> listarProductosAdmin(
            @RequestParam(required = false) @Size(max = 80) String q,
            @RequestParam(required = false) EstadoProducto estado,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean destacado,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) List<String> sort) {
        return ApiResponse.ok(servicio.listar(q, estado, categoriaId, destacado,
                Paginacion.de(page, size, sort, ORDENES, ORDEN_POR_DEFECTO)));
    }

    @PostMapping("/productos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductoAdminDto> crearProducto(@Valid @RequestBody ProductoRequest peticion) {
        return ApiResponse.ok(servicio.crear(peticion));
    }

    @GetMapping("/productos/{id}")
    @PreAuthorize("hasRole('OPERADOR')")
    public ApiResponse<ProductoAdminDto> obtenerProductoAdmin(@PathVariable Long id) {
        return ApiResponse.ok(servicio.obtener(id));
    }

    @PutMapping("/productos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductoAdminDto> editarProducto(@PathVariable Long id, @Valid @RequestBody ProductoRequest peticion) {
        return ApiResponse.ok(servicio.editar(id, peticion));
    }

    @DeleteMapping("/productos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> eliminarProducto(@PathVariable Long id) {
        servicio.eliminar(id);
        return ApiResponse.ok("Producto eliminado");
    }

    @PatchMapping("/productos/{id}/publicar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductoAdminDto> publicarProducto(@PathVariable Long id) {
        return ApiResponse.ok(servicio.publicar(id));
    }

    @PatchMapping("/productos/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductoAdminDto> desactivarProducto(@PathVariable Long id) {
        return ApiResponse.ok(servicio.desactivar(id));
    }

    @PutMapping("/productos/{id}/categorias")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductoAdminDto> asignarCategoriasProducto(@PathVariable Long id,
            @Valid @RequestBody CategoriasProductoRequest peticion) {
        return ApiResponse.ok(servicio.asignarCategorias(id, peticion.categoriaIds()));
    }

    @PatchMapping("/productos/{id}/destacado")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductoAdminDto> marcarDestacado(@PathVariable Long id, @Valid @RequestBody DestacadoRequest peticion) {
        return ApiResponse.ok(servicio.marcarDestacado(id, peticion));
    }

    @PutMapping("/productos/{id}/descuento")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductoAdminDto> configurarDescuentoProducto(@PathVariable Long id,
            @Valid @RequestBody DescuentoProductoRequest peticion) {
        return ApiResponse.ok(servicio.configurarDescuento(id, peticion));
    }

    @PostMapping("/productos/{id}/variantes")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductoAdminDto> crearVariante(@PathVariable Long id, @Valid @RequestBody VarianteRequest peticion) {
        return ApiResponse.ok(servicio.crearVariante(id, peticion));
    }

    @PostMapping("/productos/{id}/variantes/matriz")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductoAdminDto> generarMatrizVariantes(@PathVariable Long id, @Valid @RequestBody MatrizRequest peticion) {
        return ApiResponse.ok(servicio.generarMatriz(id, peticion));
    }

    @PutMapping("/variantes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductoAdminDto> editarVariante(@PathVariable Long id, @Valid @RequestBody EditarVarianteRequest peticion) {
        return ApiResponse.ok(servicio.editarVariante(id, peticion));
    }

    @DeleteMapping("/variantes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> eliminarVariante(@PathVariable Long id) {
        servicio.eliminarVariante(id);
        return ApiResponse.ok("Variante eliminada");
    }

    @PatchMapping("/variantes/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductoAdminDto> desactivarVariante(@PathVariable Long id) {
        return ApiResponse.ok(servicio.desactivarVariante(id));
    }

    @PatchMapping("/variantes/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ProductoAdminDto> activarVariante(@PathVariable Long id) {
        return ApiResponse.ok(servicio.activarVariante(id));
    }
}
