package com.fitstore.settings.configuracion.dto;

/** Tienda física: dirección, código de distrito y su nombre ("La Victoria, Lima, Lima"). */
public record TiendaFisicaDto(String direccion, String ubigeo, String ubigeoNombre, String referencia) {
}
