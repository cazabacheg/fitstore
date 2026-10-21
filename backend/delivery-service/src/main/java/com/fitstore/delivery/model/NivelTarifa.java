package com.fitstore.delivery.model;

/** Nivel de una tarifa según el largo de su código de ubigeo (D1): 2, 4 o 6 dígitos. */
public enum NivelTarifa {
    DEPARTAMENTO,
    PROVINCIA,
    DISTRITO;

    public static NivelTarifa de(String ubigeo) {
        return switch (ubigeo.length()) {
            case 2 -> DEPARTAMENTO;
            case 4 -> PROVINCIA;
            default -> DISTRITO;
        };
    }
}
