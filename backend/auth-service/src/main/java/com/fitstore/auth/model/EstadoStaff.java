package com.fitstore.auth.model;

/** Estado del staff que ve el backoffice. Se deduce de {@code activo} y {@code password_hash}; no es una columna (micro 5). */
public enum EstadoStaff {
    ACTIVO, INACTIVO, INVITACION_PENDIENTE
}
