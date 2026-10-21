package com.fitstore.delivery.service.dominio;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.delivery.model.EstadoEntrega;
import java.util.Map;

/** La acción no se puede hacer desde el estado actual de la entrega: 409 con ese estado (CA-6-23). */
public class TransicionNoPermitidaException extends ConflictoNegocioException {

    public static final String CODIGO = "TRANSICION_NO_PERMITIDA";

    public TransicionNoPermitidaException(EstadoEntrega actual) {
        super(CODIGO, "La entrega está en estado " + actual.name() + "; ese paso no se puede hacer",
                Map.of("estadoActual", actual.name()));
    }
}
