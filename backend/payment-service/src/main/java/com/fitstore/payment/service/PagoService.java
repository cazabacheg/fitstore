package com.fitstore.payment.service;

import com.fitstore.payment.dto.PaginaPago;
import java.util.UUID;

/** Consultas del cliente sobre su pago: la página de pago (HU-5-02) y la descarga de su boleta. */
public interface PagoService {

    /**
     * Datos de la página de pago.
     *
     * @throws com.fitstore.common.api.FitstoreException 404 {@code PREPARANDO_PAGO} si la intención aún no existe;
     *                                                   404 {@code NO_ENCONTRADO} si el pedido es de otra clienta
     */
    PaginaPago obtenerPagina(String codigoPedido, UUID clienteId);
}
