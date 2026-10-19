package com.fitstore.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.gateway.pruebas.EntornoGateway;
import com.fitstore.gateway.pruebas.ServiciosSimulados;
import com.fitstore.gateway.pruebas.TokensDePrueba;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fitstore.gateway.pruebas.PruebaGateway;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** CA-0a-10: el gateway valida el JWT (firma y vencimiento) cuando la petición trae Authorization. */
@PruebaGateway
class GatewayJwtTest {

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        EntornoGateway.registrar(registro);
    }

    @LocalServerPort
    int puerto;

    WebTestClient cliente;

    @BeforeEach
    void preparar() {
        EntornoGateway.SERVICIOS.reiniciarPeticiones();
        cliente = WebTestClient.bindToServer().baseUrl("http://localhost:" + puerto).build();
    }

    private void verificarQueNingunServicioRecibioNada() {
        for (String servicio : ServiciosSimulados.NOMBRES) {
            assertThat(EntornoGateway.SERVICIOS.de(servicio).findAll(anyRequestedFor(anyUrl()))).as(servicio).isEmpty();
        }
    }

    @Test
    @DisplayName("CA-0a-10 un token con firma inválida responde 401 con formato ApiResponse y no llega al servicio")
    void firmaInvalida() {
        String falso = new TokensDePrueba("kid-gateway").token("5", "ADMIN", Duration.ofMinutes(10)); // otra clave, mismo kid

        cliente.get().uri("/api/v1/admin/pedidos").header("Authorization", "Bearer " + falso).exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().exists("X-Request-Id")
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.codigo").isEqualTo("NO_AUTENTICADO")
                .jsonPath("$.mensaje").isNotEmpty();

        verificarQueNingunServicioRecibioNada();
    }

    @Test
    @DisplayName("CA-0a-10 un token vencido responde 401 con formato ApiResponse y no llega al servicio")
    void tokenVencido() {
        String vencido = EntornoGateway.EMISOR.token("5", "ADMIN", Duration.ofMinutes(-5));

        cliente.get().uri("/api/v1/admin/pedidos").header("Authorization", "Bearer " + vencido).exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.codigo").isEqualTo("NO_AUTENTICADO");

        verificarQueNingunServicioRecibioNada();
    }

    @Test
    @DisplayName("CA-0a-10 un token mal formado responde 401 y no llega al servicio")
    void tokenMalFormado() {
        cliente.get().uri("/api/v1/pedidos").header("Authorization", "Bearer esto-no-es-un-jwt").exchange()
                .expectStatus().isUnauthorized();

        verificarQueNingunServicioRecibioNada();
    }

    @Test
    @DisplayName("CA-0a-10 un token inválido también se rechaza en rutas públicas")
    void rutaPublicaConTokenInvalido() {
        cliente.get().uri("/api/v1/productos/polo").header("Authorization", "Bearer basura").exchange()
                .expectStatus().isUnauthorized();

        verificarQueNingunServicioRecibioNada();
    }

    @Test
    @DisplayName("CA-0a-10 un token válido llega al servicio y se reenvía tal cual")
    void tokenValido() {
        String token = EntornoGateway.EMISOR.token("42", "OPERADOR", Duration.ofMinutes(10));

        cliente.get().uri("/api/v1/admin/pedidos").header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isOk();

        List<LoggedRequest> recibidas = EntornoGateway.SERVICIOS.de("order-service").findAll(anyRequestedFor(anyUrl()));
        assertThat(recibidas).hasSize(1);
        assertThat(recibidas.get(0).getHeader("Authorization")).isEqualTo("Bearer " + token);
    }

    @Test
    @DisplayName("CA-0a-10 sin token las rutas pasan: la autorización por rol la hace cada servicio (ADR-011)")
    void sinTokenPasa() {
        cliente.get().uri("/api/v1/admin/pedidos").exchange().expectStatus().isOk();

        assertThat(EntornoGateway.SERVICIOS.de("order-service").findAll(anyRequestedFor(anyUrl()))).hasSize(1);
    }
}
