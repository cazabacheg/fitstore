package com.fitstore.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Activa la auditoría JPA ({@code creado_en} y {@code actualizado_en}). */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class AuditoriaJpaConfig {
}
