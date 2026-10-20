package com.fitstore.payment.model;

/** Estado de un intento de cobro (micro 5). */
public enum EstadoTransaccion {
    PROCESANDO,
    APROBADA,
    RECHAZADA,
    ERROR
}
