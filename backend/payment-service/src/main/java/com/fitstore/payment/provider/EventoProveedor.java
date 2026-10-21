package com.fitstore.payment.provider;

/**
 * Evento extraído de un webhook auténtico.
 *
 * @param eventoId id del aviso en la pasarela (para no procesarlo dos veces)
 * @param idPago   id del cargo al que se refiere; {@code null} si el aviso no es de un pago
 */
public record EventoProveedor(String eventoId, String idPago) {
}
