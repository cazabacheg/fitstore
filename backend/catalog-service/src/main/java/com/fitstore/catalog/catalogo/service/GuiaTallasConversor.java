package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.GuiaTallasDto;
import com.fitstore.catalog.catalogo.model.GuiaTallas;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Convierte la guía de tallas entre su DTO y el {@code jsonb} de la tabla (D6): {@code {columnas: [...], filas: [[...]]}}. */
final class GuiaTallasConversor {

    private GuiaTallasConversor() {
    }

    static GuiaTallasDto aDto(GuiaTallas guia) {
        Map<String, Object> tabla = guia.getTabla();
        return new GuiaTallasDto(textos(tabla.get("columnas")), filas(tabla.get("filas")), guia.getNota());
    }

    static Map<String, Object> aTabla(GuiaTallasDto dto) {
        Map<String, Object> tabla = new LinkedHashMap<>();
        tabla.put("columnas", dto.columnas());
        tabla.put("filas", dto.filas());
        return tabla;
    }

    private static List<String> textos(Object valor) {
        List<String> resultado = new ArrayList<>();
        if (valor instanceof List<?> lista) {
            lista.forEach(e -> resultado.add(String.valueOf(e)));
        }
        return resultado;
    }

    private static List<List<String>> filas(Object valor) {
        List<List<String>> resultado = new ArrayList<>();
        if (valor instanceof List<?> lista) {
            lista.forEach(fila -> resultado.add(textos(fila)));
        }
        return resultado;
    }
}
