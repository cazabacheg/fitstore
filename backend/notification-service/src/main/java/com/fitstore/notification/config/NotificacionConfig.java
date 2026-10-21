package com.fitstore.notification.config;

import com.fitstore.notification.envio.Pausa;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/** Beans comunes de notificaciones: reloj, pausa del ritmo, clientes HTTP con plazos cortos y los procesos programados. */
@Configuration
@EnableConfigurationProperties(PropiedadesNotificacion.class)
public class NotificacionConfig {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    Pausa pausa() {
        return Pausa.DE_VERDAD;
    }

    /** Cliente para las llamadas síncronas salientes (payment, settings): payment responde en 5 s o se reintenta. */
    @Bean
    RestClient clienteSaliente() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(fabrica).build();
    }

    /** Los {@code @Scheduled} (despachador y respaldo de la boleta) solo corren si la programación está habilitada. */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "fitstore.notificacion.programacion-habilitada", havingValue = "true",
            matchIfMissing = true)
    static class Programacion {
    }
}
