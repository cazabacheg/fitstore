package com.fitstore.payment.pruebas;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL en Testcontainers compartido por todas las pruebas de payment (se arranca una vez), con la misma etiqueta
 * que el compose ({@code postgres:17.10}). No es H2: el esquema usa índices parciales y {@code jsonb}. Se conecta con
 * propiedades, sin {@code spring-boot-testcontainers} (no está en stack-versiones).
 */
public class PostgresPrueba implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.10");

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
