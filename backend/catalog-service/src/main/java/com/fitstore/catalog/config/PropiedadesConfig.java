package com.fitstore.catalog.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra las propiedades propias de catalog. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CatalogoProperties.class, InventarioProperties.class})
public class PropiedadesConfig {
}
