package com.fitstore.common.event;

import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mensajería de un servicio, activa cuando define {@code fitstore.eventos.servicio}: topología (I4), conversor JSON
 * y consumidor idempotente. Registra el paquete de {@link EventoProcesado} para el escaneo de entidades JPA.
 */
@AutoConfiguration(afterName = "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ConditionalOnClass({RabbitTemplate.class, EventoProcesado.class})
@ConditionalOnProperty(prefix = "fitstore.eventos", name = "servicio")
@EnableConfigurationProperties(EventosProperties.class)
@AutoConfigurationPackage(basePackageClasses = EventoProcesado.class)
public class EventosAutoConfiguration {

    @Bean
    Declarables topologiaRabbit(EventosProperties propiedades) {
        return TopologiaRabbit.declarar(propiedades);
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    JacksonJsonMessageConverter conversorJsonDeEventos() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    ConsumidorIdempotente consumidorIdempotente(EventoProcesadoRepository repositorio, TransactionTemplate transaccion,
            EventosProperties propiedades) {
        return new ConsumidorIdempotente(repositorio, transaccion, propiedades);
    }
}
