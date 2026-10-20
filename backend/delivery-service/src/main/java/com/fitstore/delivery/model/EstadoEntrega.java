package com.fitstore.delivery.model;

/** Estados de una entrega: los de delivery local, los de agencia y el final {@code DEVUELTA} de ambos (micro, sección 5). */
public enum EstadoEntrega {
    PENDIENTE_ASIGNACION,
    ASIGNADA,
    EN_RUTA,
    FALLIDA,
    PENDIENTE_ENVIO,
    ENVIADO_AGENCIA,
    DISPONIBLE_EN_AGENCIA,
    ENTREGADA,
    DEVUELTA
}
