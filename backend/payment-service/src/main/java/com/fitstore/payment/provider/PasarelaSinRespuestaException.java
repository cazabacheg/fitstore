package com.fitstore.payment.provider;

/** La pasarela no respondió a tiempo o falló del lado del servidor: el cargo queda en curso y no se reintenta. */
public class PasarelaSinRespuestaException extends RuntimeException {

    public PasarelaSinRespuestaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
