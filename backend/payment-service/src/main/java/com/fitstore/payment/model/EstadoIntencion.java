package com.fitstore.payment.model;

/** Estado de la intención de pago (micro 5). */
public enum EstadoIntencion {
    PENDIENTE,
    PAGADA,
    ANULADA,
    REEMBOLSADA_PARCIAL,
    REEMBOLSADA
}
