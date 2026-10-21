package com.fitstore.payment.service;

import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Transaccion;

/** Lo que devuelve un cobro: la transacción resultante (nueva o la del intento original) y su intención. */
public record ResultadoCobro(Transaccion transaccion, IntencionPago intencion) {

    public String descripcionMedio() {
        Object d = transaccion.getRespuesta() == null ? null : transaccion.getRespuesta().get("descripcionMedio");
        return d == null ? null : d.toString();
    }
}
