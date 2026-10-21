package com.fitstore.delivery.model;

/**
 * Acciones que ofrece una entrega según su estado ({@code accionesPermitidas}, E5). {@code DEVUELTA} es parte del
 * contrato pero nunca se ofrece: la devolución a la tienda es extensión de alcance (extension-alcance.md).
 */
public enum AccionEntrega {
    ASIGNAR,
    EN_RUTA,
    ENTREGADA,
    FALLIDA,
    REPROGRAMAR,
    ENVIADA_AGENCIA,
    DISPONIBLE_AGENCIA,
    DEVUELTA
}
