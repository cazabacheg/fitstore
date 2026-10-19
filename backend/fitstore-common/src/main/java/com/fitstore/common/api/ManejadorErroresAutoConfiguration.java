package com.fitstore.common.api;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/** Registra el manejador de errores K5 si el servicio no define uno propio que extienda la base. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBooleanProperty(name = "fitstore.api.manejador-errores", matchIfMissing = true)
public class ManejadorErroresAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandlerBase.class)
    ManejadorErroresPorDefecto manejadorDeErrores() {
        return new ManejadorErroresPorDefecto();
    }
}
