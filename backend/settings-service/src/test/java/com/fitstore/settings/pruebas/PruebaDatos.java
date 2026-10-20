package com.fitstore.settings.pruebas;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Base de las pruebas de capa de datos: {@code @DataJpaTest} sobre PostgreSQL real, con Flyway y {@code validate}. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class PruebaDatos {

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        PostgresDePrueba.registrar(registro);
    }
}
