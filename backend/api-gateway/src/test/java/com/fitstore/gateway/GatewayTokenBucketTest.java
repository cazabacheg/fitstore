package com.fitstore.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.gateway.pruebas.EntornoGateway;
import com.fitstore.gateway.pruebas.PruebaGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * CA-0a-11: "50 por segundo (ráfaga 100)" es un token bucket (infraestructura 4.4): capacidad = ráfaga y reposición
 * continua. Aquí con capacidad 5 y reposición 5/s para comprobarlo en milisegundos.
 */
@PruebaGateway
class GatewayTokenBucketTest {

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        EntornoGateway.registrar(registro);
        registro.add("fitstore.gateway.rate-limit.resto.capacidad", () -> "5");
        registro.add("fitstore.gateway.rate-limit.resto.reposicion-por-segundo", () -> "5");
    }

    @LocalServerPort
    int puerto;

    @Autowired
    ReactiveStringRedisTemplate redis;

    WebTestClient cliente;

    @BeforeEach
    void preparar() {
        redis.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
        cliente = WebTestClient.bindToServer().baseUrl("http://localhost:" + puerto).build();
    }

    private int estado(String ip) {
        return cliente.get().uri("/api/v1/categorias").header("X-Forwarded-For", ip)
                .exchange().returnResult(String.class).getStatus().value();
    }

    @Test
    @DisplayName("CA-0a-11 token bucket: la ráfaga se agota, y luego los tokens se reponen de forma continua")
    void rafagaYReposicion() throws Exception {
        String ip = "198.51.100.77";
        for (int i = 1; i <= 5; i++) {
            assertThat(estado(ip)).as("ráfaga %d", i).isEqualTo(200);
        }
        // agotada la ráfaga: el siguiente se rechaza (con un tope por si la reposición alcanzó a dar 1 token)
        int rechazados = 0;
        for (int i = 0; i < 4; i++) {
            if (estado(ip) == 429) {
                rechazados++;
            }
        }
        assertThat(rechazados).isGreaterThanOrEqualTo(2);

        Thread.sleep(1100); // a 5/s se reponen ~5 tokens, topados en la capacidad (5)

        int aceptados = 0;
        for (int i = 0; i < 8; i++) {
            if (estado(ip) == 200) {
                aceptados++;
            }
        }
        // una ventana fija de 2 s no repondría nada a 1,1 s; el token bucket repuso la capacidad entera
        assertThat(aceptados).isBetween(5, 7);
    }
}
