package com.fitstore.auth.pruebas;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL en Testcontainers compartido por todas las pruebas de auth (se arranca una vez). No es H2: las
 * migraciones usan funciones de PostgreSQL. Se conecta con propiedades, sin agregar dependencias fuera de stack-versiones.
 */
public class PostgresPrueba implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    static {
        POSTGRES.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "spring.datasource.username=" + POSTGRES.getUsername(),
                "spring.datasource.password=" + POSTGRES.getPassword()).applyTo(contexto);
    }
}
