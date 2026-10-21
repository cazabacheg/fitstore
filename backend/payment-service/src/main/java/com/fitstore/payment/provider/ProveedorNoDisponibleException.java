package com.fitstore.payment.provider;

import com.fitstore.common.api.FitstoreException;
import org.springframework.http.HttpStatus;

/** La pasarela elegida no está disponible por el momento (circuito abierto, CA-5-23): 503. */
public class ProveedorNoDisponibleException extends FitstoreException {

    public static final String MENSAJE = "No disponible por el momento; elige otro medio de pago";

    public ProveedorNoDisponibleException() {
        super(HttpStatus.SERVICE_UNAVAILABLE, "PROVEEDOR_NO_DISPONIBLE", MENSAJE);
    }
}
