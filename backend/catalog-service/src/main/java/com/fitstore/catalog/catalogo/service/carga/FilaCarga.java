package com.fitstore.catalog.catalogo.service.carga;

import java.util.Map;

/** Una fila de datos del archivo: su número (la cabecera es la 1) y sus valores por nombre de columna, ya recortados. */
public record FilaCarga(int numero, Map<String, String> valores) {

    public String valor(String columna) {
        String v = valores.get(columna);
        return v == null ? "" : v.trim();
    }
}
