package com.fitstore.order.pruebas;

import com.fitstore.common.event.OutboxEvento;
import com.fitstore.common.event.OutboxEventoRepository;
import com.fitstore.order.carrito.repository.CarritoRepository;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Base de las pruebas de API de order: MockMvc, repositorios y el WireMock de las dependencias reiniciado. */
@PruebaApi
public abstract class ApiBase {

    @Autowired protected MockMvc mvc;
    @Autowired protected JsonMapper json;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected PedidoRepository pedidos;
    @Autowired protected CarritoRepository carritos;
    @Autowired protected OutboxEventoRepository outbox;
    @Autowired protected StringRedisTemplate redis;

    @BeforeEach
    void reiniciarDependencias() {
        EntornoApi.SERVICIOS.resetAll();
        // El caché de los parámetros de settings (5 min) viviría entre pruebas
        redis.delete("order:settings:public");
    }

    protected JsonNode cuerpo(org.springframework.test.web.servlet.ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    /** Eventos del outbox de un tipo, en el orden en que se registraron. */
    protected List<OutboxEvento> eventos(String tipo) {
        return outbox.findAll().stream().filter(e -> tipo.equals(e.getTipo()))
                .sorted(java.util.Comparator.comparing(OutboxEvento::getCreadoEn)).toList();
    }

    protected JsonNode datos(OutboxEvento evento) {
        return json.readTree(evento.getPayload());
    }
}
