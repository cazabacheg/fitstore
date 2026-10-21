package com.fitstore.order.pedidos.controller;

import com.fitstore.common.api.FitstoreException;
import org.springframework.http.HttpStatus;

/**
 * Los endpoints del contrato que son extensión de alcance (docs/implementacion/extension-alcance.md) conservan su
 * firma pero no tienen lógica: responden 501 {@code EXTENSION_DE_ALCANCE}.
 */
public final class ExtensionDeAlcance {

    public static final String CODIGO = "EXTENSION_DE_ALCANCE";

    private ExtensionDeAlcance() {
    }

    public static FitstoreException de(String funcion) {
        return new FitstoreException(HttpStatus.NOT_IMPLEMENTED, CODIGO,
                funcion + " todavía no está disponible: es una extensión de alcance");
    }
}
