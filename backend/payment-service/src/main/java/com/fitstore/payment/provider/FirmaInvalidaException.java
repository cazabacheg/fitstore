package com.fitstore.payment.provider;

import com.fitstore.common.api.FitstoreException;
import org.springframework.http.HttpStatus;

/** El webhook no trae una firma válida: 401 y no se procesa (CA-5-21). */
public class FirmaInvalidaException extends FitstoreException {

    public FirmaInvalidaException() {
        super(HttpStatus.UNAUTHORIZED, "FIRMA_INVALIDA", "La firma del aviso no es válida");
    }
}
