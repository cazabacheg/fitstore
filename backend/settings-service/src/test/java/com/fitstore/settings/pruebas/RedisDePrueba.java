package com.fitstore.settings.pruebas;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;

/** Redis compartido por las pruebas de integración (etiqueta exacta del compose, stack v10). */
public final class RedisDePrueba {

    public static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.10.1").withExposedPorts(6379);

    static {
        REDIS.start();
    }

    private RedisDePrueba() {
    }

    public static void registrar(DynamicPropertyRegistry registro) {
        registro.add("spring.data.redis.host", REDIS::getHost);
        registro.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
