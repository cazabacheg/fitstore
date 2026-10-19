package com.fitstore.common.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Outbox (ADR-009), activo con {@code fitstore.outbox.habilitado=true}. El servicio debe tener un bean
 * {@link CatalogoRutasEventos} y la tabla {@code outbox_evento} en su migración. Requiere
 * {@code spring.rabbitmq.publisher-confirm-type=correlated}.
 */
@AutoConfiguration(after = EventosAutoConfiguration.class,
        afterName = "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ConditionalOnClass({RabbitTemplate.class, OutboxEvento.class})
@ConditionalOnBooleanProperty("fitstore.outbox.habilitado")
@EnableConfigurationProperties({OutboxProperties.class, EventosProperties.class})
@AutoConfigurationPackage(basePackageClasses = OutboxEvento.class)
@EnableScheduling
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    OutboxRegistrador outboxRegistrador(OutboxEventoRepository repositorio, JsonMapper json) {
        return new OutboxRegistrador(repositorio, json);
    }

    @Bean
    @ConditionalOnMissingBean
    OutboxPublisher outboxPublisher(OutboxEventoRepository repositorio, RabbitTemplate rabbitTemplate,
            CatalogoRutasEventos rutas, TransactionTemplate transaccion, JsonMapper json,
            EventosProperties eventos, OutboxProperties propiedades) {
        return new OutboxPublisher(repositorio, rabbitTemplate, rutas, transaccion, json, eventos, propiedades);
    }
}
