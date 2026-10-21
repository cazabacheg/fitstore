package com.fitstore.delivery.config;

import com.fitstore.common.api.FitstoreException;
import org.springframework.http.HttpStatus;

/**
 * La función está especificada y está en el contrato, pero queda fuera del alcance de la implementación actual
 * ({@code docs/implementacion/extension-alcance.md}): responde 501 {@code EXTENSION_DE_ALCANCE}.
 */
public class ExtensionDeAlcanceException extends FitstoreException {

    public static final String CODIGO = "EXTENSION_DE_ALCANCE";

    public ExtensionDeAlcanceException() {
        super(HttpStatus.NOT_IMPLEMENTED, CODIGO, "Esta función aún no está disponible");
    }
}
