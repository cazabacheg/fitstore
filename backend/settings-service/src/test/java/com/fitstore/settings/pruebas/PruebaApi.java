package com.fitstore.settings.pruebas;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base de las pruebas de API: contexto completo con PostgreSQL y Redis reales (Testcontainers),
 * el reloj fijo en el lunes 19 de octubre de 2026 (hora de Lima) y el almacén de archivos en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PruebaApi.RelojFijo.class)
public abstract class PruebaApi {

    /** Lunes 19 de octubre de 2026, 10:00 en Lima. */
    public static final Instant AHORA = Instant.parse("2026-10-19T15:00:00Z");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        PostgresDePrueba.registrar(registro);
        RedisDePrueba.registrar(registro);
    }

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(AHORA, ZoneId.of("America/Lima"));
        }
    }
}
