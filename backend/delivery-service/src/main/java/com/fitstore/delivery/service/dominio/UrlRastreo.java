package com.fitstore.delivery.service.dominio;

import com.fitstore.delivery.model.Agencia;

/** Arma el enlace de rastreo con la plantilla de la agencia (D4); sin plantilla o sin código no hay enlace. */
public final class UrlRastreo {

    private UrlRastreo() {
    }

    public static String armar(Agencia agencia, String codigoSeguimiento) {
        if (agencia == null || agencia.getUrlSeguimiento() == null || codigoSeguimiento == null
                || codigoSeguimiento.isBlank()) {
            return null;
        }
        return agencia.getUrlSeguimiento().replace("{codigo}", codigoSeguimiento.trim());
    }
}
