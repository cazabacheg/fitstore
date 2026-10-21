package com.fitstore.payment.service;

import com.fitstore.payment.provider.WebhookEntrante;

/** Webhooks de las pasarelas: verificados, guardados una sola vez y confirmados consultando la API (HU-5-07). */
public interface WebhookService {

    /**
     * Procesa un webhook de Mercado Pago.
     *
     * @throws com.fitstore.payment.provider.FirmaInvalidaException 401 si la firma {@code x-signature} no es válida (no se guarda)
     */
    void recibirMercadoPago(WebhookEntrante webhook);
}
