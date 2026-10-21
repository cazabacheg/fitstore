package com.fitstore.payment.provider;

/**
 * La pasarela respondió pero no reconoce o rechaza la consulta (por ejemplo, un pago que no existe). No es una caída de la
 * pasarela, así que no cuenta para abrir su circuito.
 */
public class ConsultaRechazadaException extends RuntimeException {

    public ConsultaRechazadaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
