package com.fitstore.order.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Clientes Feign de catalog, auth, delivery y settings, y el planificador que expira los pedidos sin pago. */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackages = "com.fitstore.order.client")
@EnableScheduling
public class FeignConfig {
}
