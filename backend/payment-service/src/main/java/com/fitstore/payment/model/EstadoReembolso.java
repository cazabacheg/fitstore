package com.fitstore.payment.model;

/** Estado de un reembolso (micro 5). {@code RESUELTO_MANUAL} cuenta como completado para la intención. */
public enum EstadoReembolso {
    PENDIENTE,
    COMPLETADO,
    FALLIDO,
    RESUELTO_MANUAL
}
