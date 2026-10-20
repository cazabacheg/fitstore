package com.fitstore.catalog.inventario.service;

import com.fitstore.catalog.catalogo.service.DisponibilidadPort;
import com.fitstore.catalog.catalogo.service.Existencias;
import com.fitstore.catalog.catalogo.service.StockVariantePort;
import com.fitstore.catalog.inventario.model.MovimientoStock;
import com.fitstore.catalog.inventario.model.Stock;
import com.fitstore.catalog.inventario.model.TipoMovimiento;
import com.fitstore.catalog.inventario.repository.MovimientoStockRepository;
import com.fitstore.catalog.inventario.repository.ReservaItemRepository;
import com.fitstore.catalog.inventario.repository.StockRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que el módulo inventario ofrece al módulo catálogo (micro 10.1): disponibilidad por variante y el ciclo de vida
 * del stock de una variante. Catálogo solo conoce los puertos, no las entidades de inventario.
 */
@Component
public class DisponibilidadAdapter implements DisponibilidadPort, StockVariantePort {

    private final StockRepository stock;
    private final MovimientoStockRepository movimientos;
    private final ReservaItemRepository reservaItems;

    public DisponibilidadAdapter(StockRepository stock, MovimientoStockRepository movimientos, ReservaItemRepository reservaItems) {
        this.stock = stock;
        this.movimientos = movimientos;
        this.reservaItems = reservaItems;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Existencias> existencias(Collection<Long> varianteIds) {
        Map<Long, Existencias> resultado = new HashMap<>();
        if (varianteIds.isEmpty()) {
            return resultado;
        }
        for (Stock s : stock.findAllById(varianteIds)) {
            resultado.put(s.getVarianteId(), new Existencias(s.getFisico(), s.getReservado()));
        }
        return resultado;
    }

    @Override
    @Transactional
    public void crearStock(Long varianteId) {
        crearStockConEntrada(varianteId, 0, null, null);
    }

    @Override
    @Transactional
    public void crearStockConEntrada(Long varianteId, int cantidad, String motivo, UUID usuarioId) {
        Stock nuevo = new Stock();
        nuevo.setVarianteId(varianteId);
        nuevo.setFisico(cantidad);
        nuevo.setReservado(0);
        stock.saveAndFlush(nuevo);
        if (cantidad > 0) {
            MovimientoStock movimiento = new MovimientoStock();
            movimiento.setVarianteId(varianteId);
            movimiento.setTipo(TipoMovimiento.ENTRADA);
            movimiento.setCantidad(cantidad);
            movimiento.setSaldoFisico(cantidad);
            movimiento.setMotivo(motivo);
            movimiento.setUsuarioId(usuarioId);
            movimientos.save(movimiento);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneMovimientosOReservas(Collection<Long> varianteIds) {
        return !varianteIds.isEmpty()
                && (movimientos.existsByVarianteIdIn(varianteIds) || reservaItems.existsByVarianteIdIn(varianteIds));
    }

    @Override
    @Transactional
    public void eliminarStock(Collection<Long> varianteIds) {
        stock.deleteAllById(varianteIds);
        stock.flush();
    }
}
