package com.fitstore.catalog.pruebas;

import com.fitstore.catalog.config.AuditoriaJpaConfig;
import com.fitstore.common.persistence.JsonPersistenciaAutoConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/** Prueba de capa de datos (AP1): {@code @DataJpaTest} con Flyway real, {@code validate} y PostgreSQL en Testcontainers. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
@Import(AuditoriaJpaConfig.class)
@ImportAutoConfiguration({JacksonAutoConfiguration.class, JsonPersistenciaAutoConfiguration.class})
public @interface PruebaRepositorio {
}
