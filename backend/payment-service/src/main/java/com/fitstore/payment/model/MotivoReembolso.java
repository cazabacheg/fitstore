package com.fitstore.payment.model;

/** Origen de un reembolso: siempre nace del pedido (micro 2.1, D4). */
public enum MotivoReembolso {
    CANCELACION,
    PAGO_NO_APLICABLE,
    POSTVENTA
}
