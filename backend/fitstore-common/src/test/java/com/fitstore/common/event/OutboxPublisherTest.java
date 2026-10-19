package com.fitstore.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import com.fitstore.common.pruebas.AplicacionDePrueba;
import com.fitstore.common.pruebas.ContenedoresPrueba;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** CA-0a-14: outbox. La fila queda publicada solo tras la confirmación de RabbitMQ; si falla, queda pendiente. */
@SpringBootTest(
        classes = {AplicacionDePrueba.class, OutboxPublisherTest.Rutas.class},
        properties = {
            "spring.sql.init.mode=always",
            "spring.sql.init.schema-locations=classpath:esquema-eventos-prueba.sql",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.rabbitmq.publisher-confirm-type=correlated",
            "fitstore.security.habilitado=false",
            "fitstore.eventos.servicio=pruebas",
            "fitstore.eventos.enlaces[0]=pruebas.cosa.creada",
            "fitstore.outbox.habilitado=true",
            "fitstore.outbox.intervalo-ms=3600000",
            "fitstore.outbox.espera-confirmacion-ms=1500"
        })
class OutboxPublisherTest {

    @Configuration
    static class Rutas {
        @Bean
        CatalogoRutasEventos catalogoRutasEventos() {
            return tipo -> switch (tipo) {
                case "CosaCreada" -> "pruebas.cosa.creada";
                default -> throw new IllegalArgumentException("Evento desconocido: " + tipo);
            };
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        ContenedoresPrueba.registrarPostgres(registro);
        ContenedoresPrueba.registrarRabbit(registro);
    }

    @Autowired OutboxRegistrador registrador;
    @Autowired OutboxPublisher publicador;
    @Autowired TransactionTemplate transaccion;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @MockitoSpyBean RabbitTemplate rabbitTemplate;

    @BeforeEach
    void limpiar() {
        Mockito.reset(rabbitTemplate);
        jdbc.update("DELETE FROM outbox_evento");
        while (rabbitTemplate.receive("pruebas.eventos", 100) != null) {
            // vaciar la cola entre pruebas
        }
    }

    private UUID registrarEvento(String agregadoId) {
        return transaccion.execute(estado -> registrador.registrar("CosaCreada", agregadoId, Map.of("nombre", "Polo")));
    }

    private boolean estaPublicado(UUID id) {
        return jdbc.queryForObject("SELECT publicado_en IS NOT NULL FROM outbox_evento WHERE id = ?", Boolean.class, id);
    }

    @Test
    @DisplayName("CA-0a-14 la fila queda con publicado_en cuando RabbitMQ confirma y el mensaje lleva el sobre K12")
    void publicaYMarcaTrasLaConfirmacion() throws Exception {
        UUID id = registrarEvento("c-1");
        assertThat(estaPublicado(id)).isFalse();

        assertThat(publicador.publicarPendientes()).isEqualTo(1);
        assertThat(estaPublicado(id)).isTrue();

        Message mensaje = rabbitTemplate.receive("pruebas.eventos", 5000);
        assertThat(mensaje).isNotNull();
        JsonNode sobre = json.readTree(mensaje.getBody());
        assertThat(sobre.get("eventoId").asString()).isEqualTo(id.toString());
        assertThat(sobre.get("tipo").asString()).isEqualTo("CosaCreada");
        assertThat(sobre.get("version").asInt()).isEqualTo(1);
        assertThat(sobre.get("ocurridoEn").asString()).isNotBlank();
        assertThat(sobre.get("datos").get("nombre").asString()).isEqualTo("Polo");
        assertThat(mensaje.getMessageProperties().getReceivedRoutingKey()).isEqualTo("pruebas.cosa.creada");
        assertThat(mensaje.getMessageProperties().getReceivedDeliveryMode().name()).isEqualTo("PERSISTENT");
    }

    @Test
    @DisplayName("CA-0a-14 si RabbitMQ no está disponible la fila queda pendiente y se publica cuando vuelve")
    void quedaPendienteSiRabbitCae() {
        UUID id = registrarEvento("c-2");
        doThrow(new AmqpConnectException(new RuntimeException("sin conexión")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        int publicados = publicador.publicarPendientes();

        assertThat(publicados).isZero();
        assertThat(estaPublicado(id)).isFalse();

        Mockito.reset(rabbitTemplate); // RabbitMQ vuelve
        assertThat(publicador.publicarPendientes()).isEqualTo(1);
        assertThat(estaPublicado(id)).isTrue();
    }

    @Test
    @DisplayName("CA-0a-14 sin confirmación de RabbitMQ la fila no se marca como publicada")
    void sinConfirmacionNoSeMarca() {
        UUID id = registrarEvento("c-3");
        doNothing().when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        int publicados = publicador.publicarPendientes();

        assertThat(publicados).isZero();
        assertThat(estaPublicado(id)).isFalse();
    }

    @Test
    @DisplayName("CA-0a-14 un tipo de evento sin routing key no bloquea los demás pendientes")
    void tipoDesconocidoNoBloquea() {
        transaccion.executeWithoutResult(e -> registrador.registrar("EventoInexistente", "x", Map.of()));
        UUID bueno = registrarEvento("c-4");

        publicador.publicarPendientes();

        assertThat(estaPublicado(bueno)).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM outbox_evento WHERE publicado_en IS NULL", Integer.class)).isEqualTo(1);
    }
}
