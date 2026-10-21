package com.fitstore.catalog.inventario.service;

import com.fitstore.catalog.inventario.dto.AjusteRequest;
import com.fitstore.catalog.inventario.dto.ConfiguracionInventarioDto;
import com.fitstore.catalog.inventario.dto.EntradaRequest;
import com.fitstore.catalog.inventario.dto.KardexDto;
import com.fitstore.catalog.inventario.dto.StockDto;
import com.fitstore.common.api.PaginaResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/** Control del stock: entradas, ajustes, kardex, mínimos y configuración (HU-2-09, micro 4.8). */
public interface InventarioService {

    PaginaResponse<StockDto> listar(String q, Long productoId, boolean soloStockBajo, boolean soloAgotados, int pagina, int tamano,
            List<String> orden);

    KardexDto kardex(Long varianteId, Pageable paginacion);

    /** Ingreso de mercadería: todo o nada, un movimiento ENTRADA por línea con el usuario y el motivo (CA-2-39). */
    List<StockDto> registrarEntrada(EntradaRequest peticion, UUID usuarioId);

    /** Ajuste positivo o negativo con motivo; no puede dejar el físico por debajo de lo reservado (CA-2-40). */
    StockDto registrarAjuste(AjusteRequest peticion, UUID usuarioId);

    /** Umbral propio del SKU; {@code null} vuelve al de la tienda (CA-2-43). */
    StockDto cambiarMinimo(Long varianteId, Integer minimo);

    ConfiguracionInventarioDto obtenerConfiguracion();

    ConfiguracionInventarioDto guardarConfiguracion(ConfiguracionInventarioDto configuracion);
}
