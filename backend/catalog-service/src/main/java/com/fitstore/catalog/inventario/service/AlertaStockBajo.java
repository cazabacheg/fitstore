package com.fitstore.catalog.inventario.service;

import com.fitstore.catalog.event.StockBajo;
import com.fitstore.catalog.inventario.repository.StockConsultas;
import com.fitstore.common.event.OutboxRegistrador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alerta de stock bajo (micro 4.8): publica {@code StockBajo} solo cuando el disponible <strong>cruza</strong> el umbral
 * hacia abajo (antes por encima del mínimo, después igual o por debajo), no en cada venta. El evento se guarda en el outbox
 * dentro de la misma transacción que cambió el stock.
 */
@Component
public class AlertaStockBajo {

    private static final Logger LOG = LoggerFactory.getLogger(AlertaStockBajo.class);

    private final StockConsultas consultas;
    private final ObjectProvider<OutboxRegistrador> outbox;

    public AlertaStockBajo(StockConsultas consultas, ObjectProvider<OutboxRegistrador> outbox) {
        this.consultas = consultas;
        this.outbox = outbox;
    }

    /**
     * @param disponibleDespues disponible ya con el cambio aplicado
     * @param cambioDisponible  cuánto cambió el disponible con esta operación (negativo si bajó)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void evaluar(Long varianteId, int disponibleDespues, int cambioDisponible) {
        if (cambioDisponible >= 0) {
            return;
        }
        int antes = disponibleDespues - cambioDisponible;
        consultas.datosAlerta(varianteId).ifPresent(datos -> {
            if (antes > datos.minimo() && disponibleDespues <= datos.minimo()) {
                OutboxRegistrador registrador = outbox.getIfAvailable();
                if (registrador == null) {
                    LOG.warn("Outbox apagado: no se publica StockBajo de la variante {}", varianteId);
                    return;
                }
                registrador.registrar("StockBajo", String.valueOf(varianteId), new StockBajo(varianteId, datos.sku(),
                        datos.producto(), datos.talla(), datos.color(), disponibleDespues, datos.minimo()));
            }
        });
    }
}
