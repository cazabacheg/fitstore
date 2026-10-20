package com.fitstore.notification.pruebas;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Arranca una sola vez el PostgreSQL de las pruebas, con la misma etiqueta del compose (stack-versiones, sección 4), y
 * registra el datasource del contexto. No se usa {@code @ServiceConnection} para no depender de otra etiqueta.
 */
public class PostgresInicializador implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.10");

    static {
        POSTGRES.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        TestPropertyValues.of(
                "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "spring.datasource.username=" + POSTGRES.getUsername(),
                "spring.datasource.password=" + POSTGRES.getPassword())
                .applyTo(contexto.getEnvironment());
    }
}
