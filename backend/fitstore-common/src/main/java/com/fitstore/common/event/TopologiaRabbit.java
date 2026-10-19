package com.fitstore.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;

/**
 * Topología de RabbitMQ de un servicio (SPEC-0a I4): el exchange {@code fitstore.eventos} (topic, durable), su cola
 * {@code {servicio}.eventos} con los enlaces que le interesan y su cola de mensajes muertos {@code .dlq}.
 */
public final class TopologiaRabbit {

    private TopologiaRabbit() {
    }

    public static Declarables declarar(EventosProperties propiedades) {
        TopicExchange exchange = new TopicExchange(propiedades.exchange(), true, false);
        Queue cola = QueueBuilder.durable(propiedades.cola())
                .withArguments(Map.of(
                        "x-dead-letter-exchange", "", // exchange por defecto: enruta directo a la cola por su nombre
                        "x-dead-letter-routing-key", propiedades.colaMuertos()))
                .build();
        Queue muertos = QueueBuilder.durable(propiedades.colaMuertos()).build();

        List<Declarable> todo = new ArrayList<>(List.of(exchange, cola, muertos));
        for (String routingKey : propiedades.enlaces()) {
            Binding enlace = BindingBuilder.bind(cola).to(exchange).with(routingKey);
            todo.add(enlace);
        }
        return new Declarables(todo);
    }
}
