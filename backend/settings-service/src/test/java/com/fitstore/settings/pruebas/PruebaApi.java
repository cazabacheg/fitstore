package com.fitstore.settings.pruebas;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Pruebas de API con PostgreSQL y Redis reales (Testcontainers). */
public abstract class PruebaApi extends PruebaApiBase {

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        RedisDePrueba.registrar(registro);
    }
}
