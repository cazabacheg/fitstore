package com.fitstore.gateway.pruebas;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Arranca el gateway completo con sus rutas reales ({@code infra/config-repo/api-gateway.yml}) sin config-server ni
 * Eureka. Estas propiedades deben ir en la anotación (no en {@code @DynamicPropertySource}) porque las lee el
 * procesamiento de {@code spring.config.import}, que ocurre antes.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.profiles.active=sin-config-server",
    "spring.config.import=file:../../infra/config-repo/api-gateway.yml",
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.username=prueba",
    "spring.cloud.config.password=prueba",
    "eureka.client.enabled=false"
})
public @interface PruebaGateway {
}
