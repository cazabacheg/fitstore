package com.fitstore.catalog.inventario.service;

import com.fitstore.catalog.inventario.model.MovimientoStock;
import com.fitstore.catalog.inventario.model.TipoMovimiento;
import com.fitstore.catalog.inventario.repository.MovimientoStockRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Deja el movimiento del kardex de cada cambio del stock físico (invariante 9: usuario, motivo y saldo resultante). */
@Component
public class RegistroMovimientos {

    private final MovimientoStockRepository movimientos;

    public RegistroMovimientos(MovimientoStockRepository movimientos) {
        this.movimientos = movimientos;
    }

    /** @param usuarioId quién lo hizo; {@code null} = el sistema */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Long varianteId, TipoMovimiento tipo, int cantidad, int saldoFisico, String motivo, String referencia,
            UUID usuarioId) {
        MovimientoStock m = new MovimientoStock();
        m.setVarianteId(varianteId);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setSaldoFisico(saldoFisico);
        m.setMotivo(motivo);
        m.setReferencia(referencia);
        m.setUsuarioId(usuarioId);
        movimientos.saveAndFlush(m);
    }
}
