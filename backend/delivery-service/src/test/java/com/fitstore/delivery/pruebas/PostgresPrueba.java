package com.fitstore.delivery.pruebas;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL compartido por todas las pruebas del módulo, con la misma etiqueta que el compose ({@code postgres:17.10}).
 * Se registra con un {@link ApplicationContextInitializer} en vez de {@code @ServiceConnection}.
 */
public final class PostgresPrueba implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.10")
            .withDatabaseName("delivery_db");

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
