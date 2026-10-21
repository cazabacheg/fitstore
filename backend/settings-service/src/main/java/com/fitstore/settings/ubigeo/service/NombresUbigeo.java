package com.fitstore.settings.ubigeo.service;

import java.util.Locale;
import java.util.Set;

/** El INEI escribe los nombres en mayúsculas; las pantallas los muestran como nombres propios. */
final class NombresUbigeo {

    private static final Set<String> CONECTORES = Set.of("de", "del", "la", "las", "los", "y", "e");

    private NombresUbigeo() {
    }

    /** "SAN JUAN DE MIRAFLORES" pasa a "San Juan de Miraflores". */
    static String formatear(String nombre) {
        String[] palabras = nombre.toLowerCase(Locale.ROOT).split(" ", -1);
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < palabras.length; i++) {
            String palabra = palabras[i];
            if (i > 0) {
                resultado.append(' ');
            }
            if (i > 0 && CONECTORES.contains(palabra) || palabra.isEmpty()) {
                resultado.append(palabra);
            } else {
                resultado.append(mayuscularInicial(palabra));
            }
        }
        return resultado.toString();
    }

    private static String mayuscularInicial(String palabra) {
        // Con paréntesis o comillas pegadas ("(ANTIGUO") la inicial es la primera letra
        int inicio = 0;
        while (inicio < palabra.length() && !Character.isLetter(palabra.charAt(inicio))) {
            inicio++;
        }
        if (inicio >= palabra.length()) {
            return palabra;
        }
        return palabra.substring(0, inicio) + Character.toUpperCase(palabra.charAt(inicio))
                + palabra.substring(inicio + 1);
    }
}
