package com.fitstore.delivery.pruebas;

import com.fitstore.common.event.OutboxEvento;
import com.fitstore.common.persistence.JsonPersistenciaAutoConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Prueba de capa de datos: {@code @DataJpaTest} sobre PostgreSQL real (Testcontainers), con Flyway ({@code V1}, {@code V2})
 * e Hibernate en {@code validate}. Cada prueba corre en una transacción que se revierte.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
@Import({JacksonAutoConfiguration.class, JsonPersistenciaAutoConfiguration.class, PruebaRepositorio.EntidadesComunes.class})
public @interface PruebaRepositorio {

    /** En la aplicación real lo registra EventosAutoConfiguration: entidades y repositorios del outbox de fitstore-common. */
    @AutoConfigurationPackage(basePackageClasses = OutboxEvento.class)
    class EntidadesComunes {
    }
}
