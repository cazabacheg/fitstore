package com.fitstore.payment.service;

import com.fitstore.payment.dto.CargoRequest;
import java.util.UUID;

/** Cobro de un pedido con bloqueo de la intención e idempotencia (T3, HU-5-06). */
public interface CobroService {

    /**
     * Cobra el monto de la intención con el token de la pasarela.
     *
     * @throws com.fitstore.common.api.RecursoNoEncontradoException si el pedido no es de la clienta
     * @throws com.fitstore.common.api.FitstoreException            409 {@code YA_PAGADO}, {@code PAGO_VENCIDO},
     *                                                              {@code PAGO_ANULADO} o {@code PAGO_EN_CURSO}; 400 si el medio no está disponible
     */
    ResultadoCobro cobrar(String codigoPedido, UUID clienteId, CargoRequest solicitud, String idempotencyKey);
}
