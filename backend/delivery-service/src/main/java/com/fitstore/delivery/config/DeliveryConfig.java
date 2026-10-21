package com.fitstore.delivery.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans propios de delivery. */
@Configuration
@EnableConfigurationProperties(PropiedadesDelivery.class)
public class DeliveryConfig {

    /** Zona de la tienda: "hoy" para la fecha programada es la fecha de Lima (E7). */
    public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Bean
    Clock reloj() {
        return Clock.system(ZONA_LIMA);
    }
}
