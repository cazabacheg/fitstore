package com.fitstore.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.gateway.pruebas.EntornoGateway;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** CA-0a-11: rate limiting con Redis (ADR-020, infraestructura 4.4). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRateLimitTest {

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        EntornoGateway.registrar(registro);
        // ventana larga para que la prueba no dependa de cuándo cambia el segundo
        registro.add("fitstore.gateway.rate-limit.resto.ventana-segundos", () -> "60");
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

    private WebTestClient.ResponseSpec login(String ip) {
        return cliente.post().uri("/api/v1/auth/login").header("X-Forwarded-For", ip)
                .bodyValue("{\"email\":\"a@b.pe\",\"password\":\"x\"}").exchange();
    }

    @Test
    @DisplayName("CA-0a-11 el sexto POST /auth/login desde la misma IP en un minuto recibe 429 con Retry-After")
    void sextoLoginRecibe429() {
        for (int i = 1; i <= 5; i++) {
            login("203.0.113.7").expectStatus().isOk();
        }

        var respuesta = login("203.0.113.7")
                .expectStatus().isEqualTo(429)
                .expectHeader().exists("Retry-After")
                .expectHeader().exists("X-Request-Id")
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.mensaje").isEqualTo("Demasiados intentos, espera un momento")
                .jsonPath("$.codigo").isEqualTo("DEMASIADOS_INTENTOS")
                .returnResult();

        long segundos = Long.parseLong(respuesta.getResponseHeaders().getFirst("Retry-After"));
        assertThat(segundos).isBetween(1L, 60L);
    }

    @Test
    @DisplayName("CA-0a-11 el límite es por IP: otra IP sigue pasando")
    void otraIpNoSeAfecta() {
        for (int i = 1; i <= 6; i++) {
            login("203.0.113.8");
        }

        login("203.0.113.8").expectStatus().isEqualTo(429);
        login("203.0.113.9").expectStatus().isOk();
    }

    @Test
    @DisplayName("CA-0a-11 el login de staff comparte el límite de 5 por minuto y el registro tiene 3")
    void limitesPorRuta() {
        for (int i = 1; i <= 5; i++) {
            cliente.post().uri("/api/v1/auth/staff/login").header("X-Forwarded-For", "198.51.100.1")
                    .bodyValue("{}").exchange().expectStatus().isOk();
        }
        cliente.post().uri("/api/v1/auth/staff/login").header("X-Forwarded-For", "198.51.100.1")
                .bodyValue("{}").exchange().expectStatus().isEqualTo(429);

        for (int i = 1; i <= 3; i++) {
            cliente.post().uri("/api/v1/auth/registro").header("X-Forwarded-For", "198.51.100.2")
                    .bodyValue("{}").exchange().expectStatus().isOk();
        }
        cliente.post().uri("/api/v1/auth/registro").header("X-Forwarded-For", "198.51.100.2")
                .bodyValue("{}").exchange().expectStatus().isEqualTo(429);
    }

    @Test
    @DisplayName("CA-0a-11 olvido de contraseña (clientes y staff) y reclamos permiten 3 por minuto")
    void olvidoYReclamos() {
        for (String ruta : new String[] {"/api/v1/auth/password/olvido", "/api/v1/auth/staff/password/olvido", "/api/v1/reclamos"}) {
            String ip = "192.0.2." + Math.abs(ruta.hashCode() % 200);
            for (int i = 1; i <= 3; i++) {
                cliente.post().uri(ruta).header("X-Forwarded-For", ip).bodyValue("{}").exchange()
                        .expectStatus().isOk();
            }
            cliente.post().uri(ruta).header("X-Forwarded-For", ip).bodyValue("{}").exchange()
                    .expectStatus().isEqualTo(429);
        }
    }

    @Test
    @DisplayName("CA-0a-11 el checkout (POST /pedidos) permite 10 por minuto por usuario, no por IP")
    void checkoutPorUsuario() {
        String tokenA = EntornoGateway.EMISOR.token("usuario-a", "CLIENTE", Duration.ofMinutes(10));
        String tokenB = EntornoGateway.EMISOR.token("usuario-b", "CLIENTE", Duration.ofMinutes(10));

        for (int i = 1; i <= 10; i++) {
            cliente.post().uri("/api/v1/pedidos").header("Authorization", "Bearer " + tokenA)
                    .header("X-Forwarded-For", "203.0.113.50").bodyValue("{}").exchange().expectStatus().isOk();
        }
        cliente.post().uri("/api/v1/pedidos").header("Authorization", "Bearer " + tokenA)
                .header("X-Forwarded-For", "203.0.113.50").bodyValue("{}").exchange()
                .expectStatus().isEqualTo(429).expectHeader().exists("Retry-After");

        // otro usuario desde la misma IP no se ve afectado
        cliente.post().uri("/api/v1/pedidos").header("Authorization", "Bearer " + tokenB)
                .header("X-Forwarded-For", "203.0.113.50").bodyValue("{}").exchange().expectStatus().isOk();
    }

    @Test
    @DisplayName("CA-0a-11 los cargos (POST /pagos/{pedido}/cargos) permiten 10 por minuto por usuario")
    void cargosPorUsuario() {
        String token = EntornoGateway.EMISOR.token("usuario-c", "CLIENTE", Duration.ofMinutes(10));

        for (int i = 1; i <= 10; i++) {
            cliente.post().uri("/api/v1/pagos/FS-000001/cargos").header("Authorization", "Bearer " + token)
                    .bodyValue("{}").exchange().expectStatus().isOk();
        }
        cliente.post().uri("/api/v1/pagos/FS-000001/cargos").header("Authorization", "Bearer " + token)
                .bodyValue("{}").exchange().expectStatus().isEqualTo(429);
    }

    @Test
    @DisplayName("CA-0a-11 el resto de las rutas permite una ráfaga de 100 por ventana y luego responde 429")
    void restoDeRutas() {
        int aceptadas = 0;
        int rechazadas = 0;
        for (int i = 1; i <= 130; i++) {
            var estado = cliente.get().uri("/api/v1/categorias").header("X-Forwarded-For", "203.0.113.99")
                    .exchange().returnResult(String.class).getStatus().value();
            if (estado == 200) {
                aceptadas++;
            } else if (estado == 429) {
                rechazadas++;
            }
        }
        assertThat(aceptadas).isEqualTo(100);
        assertThat(rechazadas).isEqualTo(30);
    }
}
