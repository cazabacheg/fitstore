package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.CotizacionDto;
import com.fitstore.catalog.catalogo.dto.LineaCotizarRequest;
import java.util.List;

/** Cotización de precios, descuentos por volumen y disponibilidad para el carrito y el checkout (micro 4.5). */
public interface CotizacionService {

    /**
     * Cotiza las líneas: una variante inactiva o de un producto no ACTIVO vuelve {@code NO_DISPONIBLE} sin afectar a las demás.
     * El stock exacto no se expone: solo se informa {@code disponible} cuando lo pedido no alcanza.
     */
    CotizacionDto cotizar(List<LineaCotizarRequest> lineas);
}
