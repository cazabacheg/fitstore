package com.fitstore.payment.provider;

import com.fitstore.payment.model.EstadoTransaccion;
import java.util.Map;

/**
 * Resultado de un cargo traducido por el adaptador.
 *
 * @param codigo           código de la pasarela (p. ej. {@code cc_rejected_insufficient_amount})
 * @param mensajeLegible   mensaje en español para el cliente
 * @param descripcionMedio p. ej. "Visa ****1111"
 * @param respuesta        datos de la respuesta que se guardan, sin datos sensibles
 */
public record ResultadoCargo(EstadoTransaccion estado, String idProveedor, String codigo, String mensajeLegible,
        String descripcionMedio, Map<String, Object> respuesta) {
}
