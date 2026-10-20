package com.fitstore.payment.event;

/** Motivo declarado por order en una solicitud de reembolso postventa. */
public enum MotivoPostventa {
    DEFECTUOSO,
    TALLA_MODELO_EQUIVOCADO,
    NO_CONFORME,
    NO_ENTREGADO,
    OTRO
}
