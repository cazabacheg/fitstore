package com.fitstore.auth.service;

import java.util.Locale;

/** Normalización del correo (invariante 1): {@code trim} y minúsculas. */
public final class Correo {

    private Correo() {
    }

    public static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
