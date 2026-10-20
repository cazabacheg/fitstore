package com.fitstore.common.pruebas;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/** PostgreSQL y RabbitMQ compartidos por todas las pruebas de integración de la librería (se arrancan una vez). */
public final class ContenedoresPrueba {

    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17.10");
    public static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4.3.5-management");

    static {
        POSTGRES.start();
        RABBIT.start();
    }

    private ContenedoresPrueba() {
    }

    public static void registrarPostgres(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    public static void registrarRabbit(DynamicPropertyRegistry registro) {
        registro.add("spring.rabbitmq.host", RABBIT::getHost);
        registro.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registro.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registro.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
    }
}
