package com.fitstore.catalog.pruebas;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL propio para las pruebas que aplican los datos de demostración ({@code db/dev}): esas filas no deben llegar a la
 * base que comparten las demás pruebas (y Flyway rechazaría una base con migraciones repetibles que el contexto no conoce).
 */
public class PostgresAparte implements ApplicationContextInitializer<ConfigurableApplicationContext> {

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
