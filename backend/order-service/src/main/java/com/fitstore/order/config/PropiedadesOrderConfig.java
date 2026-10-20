package com.fitstore.order.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Activa las propiedades {@code fitstore.order.*}. */
@Configuration
@EnableConfigurationProperties(PropiedadesOrder.class)
public class PropiedadesOrderConfig {
}
