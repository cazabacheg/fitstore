package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.Talla;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;

/**
 * Formato del SKU de una variante (SPEC-2 C2): {@code {CODIGO}-{COL}-{TALLA}}. {@code COL} son las 3 primeras letras del
 * color en mayúsculas y sin tildes ({@code NEG}, {@code AZU}); si otro color comparte esas letras, el siguiente por orden de
 * creación lleva un dígito ({@code AZU2}, {@code AZU3}).
 */
final class CodigosSku {

    private CodigosSku() {
    }

    static String generar(String codigoProducto, Color color, List<Color> todosLosColores, Talla talla) {
        return codigoProducto + "-" + codigoDeColor(color, todosLosColores) + "-" + talla.getCodigo();
    }

    static String codigoDeColor(Color color, List<Color> todos) {
        String prefijo = prefijo(color.getNombre());
        List<Color> conElMismoPrefijo = todos.stream().filter(c -> prefijo(c.getNombre()).equals(prefijo))
                .sorted(Comparator.comparing(Color::getId)).toList();
        int posicion = 0;
        for (int i = 0; i < conElMismoPrefijo.size(); i++) {
            if (conElMismoPrefijo.get(i).getId().equals(color.getId())) {
                posicion = i;
            }
        }
        return posicion == 0 ? prefijo : prefijo + (posicion + 1);
    }

    private static String prefijo(String nombre) {
        String letras = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z]", "").toUpperCase();
        return letras.length() <= 3 ? letras : letras.substring(0, 3);
    }
}
