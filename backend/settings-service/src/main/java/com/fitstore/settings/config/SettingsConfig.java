package com.fitstore.settings.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans propios de settings. */
@Configuration
@EnableConfigurationProperties(PropiedadesSettings.class)
public class SettingsConfig {

    /** Zona de la tienda: los plazos del Libro de Reclamaciones se cuentan con la fecha de Lima. */
    public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Bean
    Clock reloj() {
        return Clock.system(ZONA_LIMA);
    }
}
