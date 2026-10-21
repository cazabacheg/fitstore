package com.fitstore.catalog.catalogo.service.dominio;

import com.fitstore.catalog.catalogo.model.ModoDescuento;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Política de descuentos vigente (micro 4.5): el interruptor global, los tramos globales y, por producto, su modo y
 * sus tramos propios. Es solo datos: no consulta nada.
 *
 * @param activa     interruptor global; apagado = ningún producto lleva descuento
 * @param global     tramos de la política global
 * @param modos      modo de cada producto (los que no aparecen usan {@link ModoDescuento#GLOBAL})
 * @param porProducto tramos propios de los productos en modo {@link ModoDescuento#PERSONALIZADO}
 */
public record PoliticaDescuentos(boolean activa, List<Tramo> global, Map<Long, ModoDescuento> modos,
        Map<Long, List<Tramo>> porProducto) {

    /** Tramos que le tocan al producto, de menor a mayor número de unidades (vacío si no lleva descuento). */
    public List<Tramo> tramosDe(Long productoId) {
        if (!activa) {
            return List.of();
        }
        ModoDescuento modo = modos.getOrDefault(productoId, ModoDescuento.GLOBAL);
        List<Tramo> tramos = switch (modo) {
            case GLOBAL -> global;
            case PERSONALIZADO -> porProducto.getOrDefault(productoId, List.of());
            case SIN_DESCUENTO -> List.of();
        };
        return tramos.stream().sorted(Comparator.comparingInt(Tramo::unidadesMinimas)).toList();
    }
}
