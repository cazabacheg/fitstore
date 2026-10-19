package com.fitstore.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.fitstore.gateway.pruebas.EntornoGateway;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fitstore.gateway.pruebas.PruebaGateway;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** CORS (solo storefront y backoffice) y tiempos (respuesta 10 s → 503), infraestructura 4.3 y 4.5. */
@PruebaGateway
class GatewayCorsYTiemposTest {

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        EntornoGateway.registrar(registro);
        registro.add("STOREFRONT_URL", () -> "http://localhost:4200");
        registro.add("BACKOFFICE_URL", () -> "http://localhost:4300");
    }

    @LocalServerPort
    int puerto;

    WebTestClient cliente;

    @BeforeEach
    void preparar() {
        EntornoGateway.SERVICIOS.reiniciarPeticiones();
        cliente = WebTestClient.bindToServer().baseUrl("http://localhost:" + puerto)
                .responseTimeout(Duration.ofSeconds(40)).build();
    }

    @AfterEach
    void restaurar() {
        EntornoGateway.SERVICIOS.de("catalog-service").resetMappings();
        EntornoGateway.SERVICIOS.de("catalog-service").stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.any(com.github.tomakehurst.wiremock.client.WireMock.anyUrl())
                        .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                                .withBody("{\"success\":true,\"mensaje\":\"Operación exitosa\",\"data\":{\"servicio\":\"catalog-service\"}}")));
    }

    @Test
    @DisplayName("CORS: el storefront y el backoffice pasan el preflight")
    void origenesPermitidos() {
        for (String origen : new String[] {"http://localhost:4200", "http://localhost:4300"}) {
            cliente.options().uri("/api/v1/categorias")
                    .header("Origin", origen)
                    .header("Access-Control-Request-Method", "GET")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("Access-Control-Allow-Origin", origen);
        }
    }

    @Test
    @DisplayName("CORS: otro origen es rechazado en el preflight")
    void origenNoPermitido() {
        cliente.options().uri("/api/v1/categorias")
                .header("Origin", "https://sitio-malicioso.example")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    }

    @Test
    @DisplayName("CA-0a-12 si el servicio no responde en 10 s el gateway responde 503")
    void servicioLento() {
        EntornoGateway.SERVICIOS.de("catalog-service").stubFor(get(urlEqualTo("/api/v1/productos/lento"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(12_000)));

        cliente.get().uri("/api/v1/productos/lento").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("Servicio no disponible, inténtalo en un momento");
    }
}
