package com.fitstore.catalog.catalogo.service;

import java.text.Normalizer;
import java.util.function.Predicate;

/** Generación de slugs (micro 2.2): sin tildes, en minúsculas y con guiones; únicos con un sufijo numérico. */
public final class Slugs {

    private Slugs() {
    }

    public static String deNombre(String nombre) {
        String sinTildes = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = sinTildes.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "sin-nombre" : slug;
    }

    /** El slug de {@code nombre}; si ya existe agrega -2, -3... hasta que no choque. */
    public static String unico(String nombre, int largoMaximo, Predicate<String> existe) {
        String base = deNombre(nombre);
        if (base.length() > largoMaximo - 5) {
            base = base.substring(0, largoMaximo - 5).replaceAll("-+$", "");
        }
        String candidato = base;
        int n = 2;
        while (existe.test(candidato)) {
            candidato = base + "-" + n++;
        }
        return candidato;
    }
}
