package com.fitstore.order.pruebas;

import com.fitstore.common.event.OutboxEvento;
import com.fitstore.common.event.OutboxEventoRepository;
import com.fitstore.order.carrito.repository.CarritoRepository;
import com.fitstore.order.pedidos.repository.PedidoRepository;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.Pedido;
import java.util.List;
import java.util.UUID;
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
    @Autowired protected jakarta.persistence.EntityManager em;

    @BeforeEach
    void reiniciarDependencias() {
        EntornoApi.SERVICIOS.resetAll();
        // El caché de los parámetros de settings (5 min) viviría entre pruebas
        redis.delete("order:settings:public");
    }

    protected JsonNode cuerpo(org.springframework.test.web.servlet.ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    private static final java.util.concurrent.atomic.AtomicInteger NUMERO = new java.util.concurrent.atomic.AtomicInteger(1000);

    /** Pedido guardado en la transacción de la prueba; de recojo (sin dirección) o de delivery/agencia. */
    protected Pedido pedido(UUID cliente, EstadoPedido estado, Modalidad modalidad) {
        String codigo = "FS-" + String.format("%06d", NUMERO.incrementAndGet());
        Pedido p = modalidad == Modalidad.RECOJO ? Datos.pedidoRecojo(codigo, cliente, "k-" + codigo)
                : Datos.pedido(codigo, cliente, "k-" + codigo);
        p.setModalidad(modalidad);
        p.setEstado(estado);
        return pedidos.saveAndFlush(p);
    }

    /**
     * Manda a la base lo escrito con JPA y vacía la memoria de Hibernate: la prueba y la petición comparten la
     * transacción, y sin esto la petición vería entidades viejas tras un cambio hecho con SQL.
     */
    protected void sincronizar() {
        em.flush();
        em.clear();
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
