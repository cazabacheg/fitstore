package com.fitstore.common.api;

/** Un campo inválido en un 400 de validación (contracts K5). */
public record ErrorCampo(String campo, String mensaje) {
}
