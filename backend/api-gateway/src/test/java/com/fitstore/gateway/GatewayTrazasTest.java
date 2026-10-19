package com.fitstore.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fitstore.gateway.pruebas.EntornoGateway;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * CA-0a-13 (trazas): el gateway exporta la traza por OTLP y propaga el contexto al servicio destino.
 * Un WireMock hace de receptor OTLP (en lugar de Jaeger); la verificación en Jaeger real la cubre el humo de 0a.3.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayTrazasTest {

    static final WireMockServer OTLP = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        OTLP.start();
        OTLP.stubFor(post(urlEqualTo("/v1/traces")).willReturn(aResponse().withStatus(200)));
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        EntornoGateway.registrar(registro);
        registro.add("management.tracing.export.enabled", () -> "true");
        registro.add("management.tracing.sampling.probability", () -> "1.0");
        registro.add("management.opentelemetry.tracing.export.otlp.endpoint", () -> "http://localhost:" + OTLP.port() + "/v1/traces");
        registro.add("management.opentelemetry.tracing.export.schedule-delay", () -> "200ms");
    }

    @AfterAll
    static void detener() {
        OTLP.stop();
    }

    @LocalServerPort
    int puerto;

    private static boolean contiene(byte[] pajar, byte[] aguja) {
        outer:
        for (int i = 0; i <= pajar.length - aguja.length; i++) {
            for (int j = 0; j < aguja.length; j++) {
                if (pajar[i + j] != aguja[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    @Test
    @DisplayName("CA-0a-13 el gateway exporta una traza por OTLP con el mismo trace id que recibe el servicio destino")
    void exportaLaTrazaYPropagaElContexto() {
        WebTestClient cliente = WebTestClient.bindToServer().baseUrl("http://localhost:" + puerto).build();

        cliente.get().uri("/api/v1/categorias").exchange().expectStatus().isOk();

        List<LoggedRequest> haciaElServicio = EntornoGateway.SERVICIOS.de("catalog-service").findAll(anyRequestedFor(anyUrl()));
        String traceparent = haciaElServicio.get(haciaElServicio.size() - 1).getHeader("traceparent");
        assertThat(traceparent).as("el servicio destino recibe el contexto de la traza").matches("00-[0-9a-f]{32}-[0-9a-f]{16}-0[0-9a-f]");
        byte[] traceId = HexFormat.of().parseHex(traceparent.split("-")[1]);
        byte[] servicioGateway = "api-gateway".getBytes(StandardCharsets.UTF_8);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<LoggedRequest> exportadas = OTLP.findAll(postRequestedFor(urlEqualTo("/v1/traces")));
            assertThat(exportadas).isNotEmpty();
            assertThat(exportadas).anySatisfy(peticion -> {
                assertThat(contiene(peticion.getBody(), servicioGateway)).as("service.name=api-gateway").isTrue();
                assertThat(contiene(peticion.getBody(), traceId)).as("trace id del servicio destino").isTrue();
            });
        });
    }
}
