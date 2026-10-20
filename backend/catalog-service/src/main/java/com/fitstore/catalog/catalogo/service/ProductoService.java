package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.DescuentoProductoRequest;
import com.fitstore.catalog.catalogo.dto.DestacadoRequest;
import com.fitstore.catalog.catalogo.dto.EditarVarianteRequest;
import com.fitstore.catalog.catalogo.dto.MatrizRequest;
import com.fitstore.catalog.catalogo.dto.ProductoAdminDto;
import com.fitstore.catalog.catalogo.dto.ProductoAdminResumenDto;
import com.fitstore.catalog.catalogo.dto.ProductoRequest;
import com.fitstore.catalog.catalogo.dto.VarianteRequest;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.common.api.PaginaResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** Productos y variantes del backoffice (HU-2-05, micro 4.2). */
public interface ProductoService {

    PaginaResponse<ProductoAdminResumenDto> listar(String q, EstadoProducto estado, Long categoriaId, Boolean destacado,
            Pageable paginacion);

    ProductoAdminDto obtener(Long id);

    /** Crea el producto en BORRADOR con slug generado y su código único (CA-2-21). */
    ProductoAdminDto crear(ProductoRequest peticion);

    /** Edita los datos con bloqueo optimista: una versión vieja responde 409. */
    ProductoAdminDto editar(Long id, ProductoRequest peticion);

    /** Solo BORRADOR sin movimientos ni reservas; si no, 409 y se usa desactivar (CA-2-26). */
    void eliminar(Long id);

    /** Pasa a ACTIVO si cumple la invariante 4; si no, 409 con la lista de faltantes (CA-2-24). */
    ProductoAdminDto publicar(Long id);

    ProductoAdminDto desactivar(Long id);

    ProductoAdminDto asignarCategorias(Long id, List<Long> categoriaIds);

    ProductoAdminDto marcarDestacado(Long id, DestacadoRequest peticion);

    /** Modo de descuento del producto y, si es PERSONALIZADO, sus tramos propios (CA-2-38). */
    ProductoAdminDto configurarDescuento(Long id, DescuentoProductoRequest peticion);

    ProductoAdminDto crearVariante(Long productoId, VarianteRequest peticion);

    /** Crea todas las combinaciones talla × color que aún no existen (CA-2-22). */
    ProductoAdminDto generarMatriz(Long productoId, MatrizRequest peticion);

    ProductoAdminDto editarVariante(Long varianteId, EditarVarianteRequest peticion);

    /** Solo sin movimientos ni reservas; si no, 409 y se desactiva (CA-2-26). */
    void eliminarVariante(Long varianteId);

    ProductoAdminDto desactivarVariante(Long varianteId);

    ProductoAdminDto activarVariante(Long varianteId);
}
