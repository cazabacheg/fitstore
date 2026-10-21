package com.fitstore.order.pruebas;

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
 * Prueba de API de order: el servicio completo con Flyway real, PostgreSQL y Redis en Testcontainers y las dependencias
 * (catalog, auth, delivery, settings) en WireMock. Cada prueba corre en su transacción y se revierte.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = EntornoApi.class)
@Transactional
public @interface PruebaApi {
}
