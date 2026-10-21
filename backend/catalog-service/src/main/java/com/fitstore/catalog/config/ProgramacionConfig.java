package com.fitstore.catalog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las tareas programadas (el liberador de reservas vencidas), con o sin outbox. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class ProgramacionConfig {
}
