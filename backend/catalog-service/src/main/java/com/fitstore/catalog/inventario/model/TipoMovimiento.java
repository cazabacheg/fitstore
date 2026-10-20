package com.fitstore.catalog.inventario.model;

/** Reservar y liberar no cambian el físico, por eso no generan movimiento (micro 3.1). */
public enum TipoMovimiento {
    ENTRADA, AJUSTE, VENTA, REINGRESO
}
