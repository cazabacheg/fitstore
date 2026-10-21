package com.fitstore.notification.pruebas;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/** Arranca una sola vez el RabbitMQ de las pruebas (misma etiqueta del compose) y activa el consumidor de eventos. */
public class RabbitInicializador implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.3.5-management");

    static {
        RABBIT.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        TestPropertyValues.of(
                "spring.rabbitmq.host=" + RABBIT.getHost(),
                "spring.rabbitmq.port=" + RABBIT.getAmqpPort(),
                "spring.rabbitmq.username=" + RABBIT.getAdminUsername(),
                "spring.rabbitmq.password=" + RABBIT.getAdminPassword(),
                "spring.rabbitmq.listener.simple.auto-startup=true")
                .applyTo(contexto.getEnvironment());
    }
}
