package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.CategoriaNodoDto;
import com.fitstore.catalog.catalogo.dto.ColorDto;
import com.fitstore.catalog.catalogo.dto.GuiaTallasDto;
import com.fitstore.catalog.catalogo.dto.ProductoDetalleDto;
import com.fitstore.catalog.catalogo.dto.ProductoTarjetaDto;
import com.fitstore.catalog.catalogo.dto.TallaDto;
import com.fitstore.common.api.PaginaResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** Lecturas públicas de la tienda: menú, listado con filtros, detalle, destacados y guía de tallas (micro 4.1 y 4.3). */
public interface CatalogoPublicoService {

    List<CategoriaNodoDto> arbolCategorias();

    GuiaTallasDto guiaTallas(String slugCategoria);

    PaginaResponse<ProductoTarjetaDto> listar(FiltroProductos filtro, Pageable paginacion);

    List<ProductoTarjetaDto> destacados();

    ProductoDetalleDto detalle(String slug);

    List<TallaDto> tallas();

    List<ColorDto> colores();
}
