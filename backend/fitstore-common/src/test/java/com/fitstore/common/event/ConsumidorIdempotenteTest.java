package com.fitstore.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fitstore.common.pruebas.AplicacionDePrueba;
import com.fitstore.common.pruebas.ContenedoresPrueba;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

/** CA-0a-15: consumidor idempotente, reintentos y cola de mensajes muertos; además la topología (I4). */
@SpringBootTest(
        classes = {AplicacionDePrueba.class, ConsumidorIdempotenteTest.Oyente.class},
        properties = {
            "spring.sql.init.mode=always",
            "spring.sql.init.schema-locations=classpath:esquema-eventos-prueba.sql",
            "spring.jpa.hibernate.ddl-auto=validate",
            "fitstore.security.habilitado=false",
            "fitstore.eventos.servicio=consumo",
            "fitstore.eventos.enlaces[0]=consumo.cosa.creada",
            "fitstore.eventos.reintentos=3",
            "fitstore.eventos.espera-inicial-ms=20",
            "fitstore.eventos.multiplicador=2"
        })
class ConsumidorIdempotenteTest {

    static final Map<UUID, AtomicInteger> INTENTOS = new ConcurrentHashMap<>();
    static final Map<UUID, AtomicInteger> EXITOS = new ConcurrentHashMap<>();
    /** Cuántos intentos fallan antes de que el manejador funcione, por evento. */
    static final Map<UUID, Integer> FALLAS = new ConcurrentHashMap<>();

    @Configuration
    static class Oyente {
        @Autowired ConsumidorIdempotente consumidor;

        @RabbitListener(queues = "consumo.eventos")
        void alRecibir(SobreEvento sobre) {
            consumidor.procesar(sobre, evento -> {
                int intento = INTENTOS.computeIfAbsent(evento.eventoId(), k -> new AtomicInteger()).incrementAndGet();
                if (intento <= FALLAS.getOrDefault(evento.eventoId(), 0)) {
                    throw new IllegalStateException("falla simulada");
                }
                EXITOS.computeIfAbsent(evento.eventoId(), k -> new AtomicInteger()).incrementAndGet();
            });
        }

        @Bean
        CatalogoRutasEventos catalogoRutasEventos() {
            return tipo -> "consumo.cosa.creada";
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        ContenedoresPrueba.registrarPostgres(registro);
        ContenedoresPrueba.registrarRabbit(registro);
    }

    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired RabbitAdmin rabbitAdmin;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    private void enviar(UUID eventoId) {
        SobreEvento sobre = new SobreEvento(eventoId, "CosaCreada", 1, Instant.now(), json.valueToTree(Map.of("a", 1)));
        rabbitTemplate.convertAndSend("fitstore.eventos", "consumo.cosa.creada", sobre);
    }

    @Test
    @DisplayName("CA-0a-15 un eventoId ya procesado se ignora sin efectos")
    void duplicadoSeIgnora() {
        UUID id = UUID.randomUUID();

        enviar(id);
        await().atMost(Duration.ofSeconds(15)).until(() -> EXITOS.containsKey(id));
        enviar(id);
        enviar(id);
        // un evento posterior distinto confirma que los duplicados ya se consumieron (la cola es FIFO)
        UUID siguiente = UUID.randomUUID();
        enviar(siguiente);
        await().atMost(Duration.ofSeconds(15)).until(() -> EXITOS.containsKey(siguiente));

        assertThat(EXITOS.get(id).get()).isEqualTo(1);
        assertThat(INTENTOS.get(id).get()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evento_procesado WHERE evento_id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-15 un mensaje que falla 3 veces termina en la cola .dlq de su servicio")
    void tresFallosVanALaDlq() {
        UUID id = UUID.randomUUID();
        FALLAS.put(id, 99);

        enviar(id);

        Message muerto = rabbitTemplate.receive("consumo.eventos.dlq", 15000);
        assertThat(muerto).isNotNull();
        assertThat(new String(muerto.getBody())).contains(id.toString());
        assertThat(INTENTOS.get(id).get()).isEqualTo(3);
        assertThat(EXITOS).doesNotContainKey(id);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM evento_procesado WHERE evento_id = ?", Integer.class, id))
                .isZero();
    }

    @Test
    @DisplayName("CA-0a-15 un fallo transitorio se reintenta y el evento se procesa una sola vez")
    void falloTransitorioSeReintenta() {
        UUID id = UUID.randomUUID();
        FALLAS.put(id, 1); // el primer intento falla y el segundo funciona

        enviar(id);

        await().atMost(Duration.ofSeconds(15)).until(() -> EXITOS.containsKey(id));
        assertThat(EXITOS.get(id).get()).isEqualTo(1);
        assertThat(INTENTOS.get(id).get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("I4 la topología declara el exchange fitstore.eventos, la cola del servicio y su DLQ")
    void topologia() {
        assertThat(rabbitAdmin.getQueueProperties("consumo.eventos")).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties("consumo.eventos.dlq")).isNotNull();
        Boolean exchangeExiste = rabbitTemplate.execute(canal -> {
            canal.exchangeDeclarePassive("fitstore.eventos");
            return true;
        });
        assertThat(exchangeExiste).isTrue();
    }
}
