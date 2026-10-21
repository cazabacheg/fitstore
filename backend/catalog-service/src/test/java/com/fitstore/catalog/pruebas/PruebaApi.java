package com.fitstore.catalog.pruebas;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de API de catalog: el servicio completo con Flyway real y PostgreSQL en Testcontainers, llamado con MockMvc.
 * Cada prueba corre en su transacción y se revierte, así que los datos de una no se ven en las demás.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
@Transactional
public @interface PruebaApi {
}
