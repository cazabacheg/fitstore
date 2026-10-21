package com.fitstore.payment.config;

import com.fitstore.payment.client.SettingsClient;
import com.fitstore.payment.provider.mercadopago.MercadoPagoProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Activa las propiedades de pagos, los clientes Feign y las tareas programadas del servicio. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesPagos.class)
@EnableFeignClients(basePackageClasses = SettingsClient.class)
@EnableScheduling
public class PagosConfig {

    /** Adaptador de Mercado Pago con la configuración técnica; sin credenciales no se ofrece en la página de pago. */
    @Bean
    MercadoPagoProvider mercadoPagoProvider(PropiedadesPagos propiedades) {
        return new MercadoPagoProvider(propiedades.mercadopago());
    }
}
