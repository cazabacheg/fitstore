package com.fitstore.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.gateway.pruebas.EntornoGateway;
import com.fitstore.gateway.pruebas.ServiciosSimulados;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.fitstore.gateway.pruebas.PruebaGateway;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** CA-0a-08, CA-0a-09, CA-0a-12 y CA-0a-13: enrutamiento, rutas internas, servicio caído y correlación. */
@PruebaGateway
class GatewayRutasTest {

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
        cliente = WebTestClient.bindToServer().baseUrl("http://localhost:" + puerto)
                .responseTimeout(Duration.ofSeconds(30)).build();
    }

    /** Tabla de infraestructura 4.2: cada prefijo con el servicio al que debe llegar. */
    static Stream<Arguments> rutas() {
        return Stream.of(
                // auth
                Arguments.of("/api/v1/auth/login", "auth-service"),
                Arguments.of("/api/v1/clientes/me", "auth-service"),
                Arguments.of("/api/v1/staff/me", "auth-service"),
                Arguments.of("/.well-known/jwks.json", "auth-service"),
                // catalog
                Arguments.of("/api/v1/categorias", "catalog-service"),
                Arguments.of("/api/v1/categorias/ropa/hijas", "catalog-service"),
                Arguments.of("/api/v1/productos/polo-dry-fit", "catalog-service"),
                Arguments.of("/api/v1/tallas", "catalog-service"),
                Arguments.of("/api/v1/colores", "catalog-service"),
                Arguments.of("/api/v1/precios/cotizar", "catalog-service"),
                Arguments.of("/api/v1/admin/categorias/3", "catalog-service"),
                Arguments.of("/api/v1/admin/productos", "catalog-service"),
                Arguments.of("/api/v1/admin/variantes/9", "catalog-service"),
                Arguments.of("/api/v1/admin/imagenes", "catalog-service"),
                Arguments.of("/api/v1/admin/tallas", "catalog-service"),
                Arguments.of("/api/v1/admin/colores", "catalog-service"),
                Arguments.of("/api/v1/admin/descuentos", "catalog-service"),
                Arguments.of("/api/v1/admin/inventario/movimientos", "catalog-service"),
                // order
                Arguments.of("/api/v1/carrito", "order-service"),
                Arguments.of("/api/v1/checkout/resumen", "order-service"),
                Arguments.of("/api/v1/pedidos/FS-000123", "order-service"),
                Arguments.of("/api/v1/admin/pedidos", "order-service"),
                // payment
                Arguments.of("/api/v1/pagos/FS-000123/cargos", "payment-service"),
                Arguments.of("/api/v1/pagos/webhooks/culqi", "payment-service"),
                Arguments.of("/api/v1/admin/pagos", "payment-service"),
                Arguments.of("/api/v1/admin/reembolsos/5", "payment-service"),
                // delivery
                Arguments.of("/api/v1/envios/cotizar", "delivery-service"),
                Arguments.of("/api/v1/agencias", "delivery-service"),
                Arguments.of("/api/v1/admin/entregas", "delivery-service"),
                Arguments.of("/api/v1/admin/repartidores", "delivery-service"),
                Arguments.of("/api/v1/admin/tarifas", "delivery-service"),
                Arguments.of("/api/v1/admin/agencias/2", "delivery-service"),
                // notification
                Arguments.of("/api/v1/admin/notificaciones", "notification-service"),
                // settings
                Arguments.of("/api/v1/settings/tienda", "settings-service"),
                Arguments.of("/api/v1/ubigeo/departamentos", "settings-service"),
                Arguments.of("/api/v1/reclamos", "settings-service"),
                Arguments.of("/api/v1/admin/settings/tema", "settings-service"),
                Arguments.of("/api/v1/admin/reclamos", "settings-service"));
    }

    @ParameterizedTest(name = "CA-0a-08 {0} llega a {1}")
    @MethodSource("rutas")
    @DisplayName("CA-0a-08 enrutamiento del gateway según la tabla 4.2")
    void enrutaAlServicioCorrecto(String ruta, String servicioEsperado) {
        cliente.get().uri(ruta).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.servicio").isEqualTo(servicioEsperado);

        for (String servicio : ServiciosSimulados.NOMBRES) {
            int esperadas = servicio.equals(servicioEsperado) ? 1 : 0;
            assertThat(EntornoGateway.SERVICIOS.de(servicio).findAll(anyRequestedFor(anyUrl())))
                    .as("peticiones recibidas por %s", servicio).hasSize(esperadas);
        }
    }

    @Test
    @DisplayName("CA-0a-08 la ruta llega al servicio con su ruta original")
    void conservaLaRuta() {
        cliente.get().uri("/api/v1/productos/polo-dry-fit?color=azul").exchange().expectStatus().isOk();

        List<LoggedRequest> recibidas = EntornoGateway.SERVICIOS.de("catalog-service").findAll(anyRequestedFor(anyUrl()));
        assertThat(recibidas).hasSize(1);
        assertThat(recibidas.get(0).getUrl()).isEqualTo("/api/v1/productos/polo-dry-fit?color=azul");
    }

    @Test
    @DisplayName("CA-0a-08 una ruta que no está en la tabla responde 404 con formato ApiError")
    void rutaDesconocida() {
        cliente.get().uri("/api/v1/no-existe").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO");
    }

    @ParameterizedTest(name = "CA-0a-09 {0} responde 404")
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "/internal/stock/reservar", "/internal", "/internal/pedidos/FS-1/pagado", "/internal/"})
    @DisplayName("CA-0a-09 cualquier /internal/** responde 404 y no llega a ningún servicio")
    void internoBloqueado(String ruta) {
        for (var metodo : List.of(org.springframework.http.HttpMethod.GET, org.springframework.http.HttpMethod.POST)) {
            cliente.method(metodo).uri(ruta).exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.codigo").isEqualTo("NO_ENCONTRADO");
        }
        for (String servicio : ServiciosSimulados.NOMBRES) {
            assertThat(EntornoGateway.SERVICIOS.de(servicio).findAll(anyRequestedFor(anyUrl()))).isEmpty();
        }
    }

    @Test
    @DisplayName("CA-0a-09 /internal/** responde 404 también con un token válido")
    void internoBloqueadoConToken() {
        String token = EntornoGateway.EMISOR.token("1", "ADMIN", Duration.ofMinutes(5));

        cliente.get().uri("/internal/stock").header("Authorization", "Bearer " + token).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("CA-0a-12 con un servicio detenido responde 503 con el mensaje de ApiResponse")
    void servicioCaido() {
        EntornoGateway.SERVICIOS.de("delivery-service").stop();
        try {
            cliente.get().uri("/api/v1/agencias").exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.success").isEqualTo(false)
                    .jsonPath("$.mensaje").isEqualTo("Servicio no disponible, inténtalo en un momento")
                    .jsonPath("$.codigo").isEqualTo("SERVICIO_NO_DISPONIBLE");
        } finally {
            EntornoGateway.SERVICIOS.de("delivery-service").start();
        }
    }

    @Test
    @DisplayName("CA-0a-12 el 503 también lleva X-Request-Id")
    void servicioCaidoConRequestId() {
        EntornoGateway.SERVICIOS.de("notification-service").stop();
        try {
            var respuesta = cliente.get().uri("/api/v1/admin/notificaciones").exchange()
                    .expectStatus().isEqualTo(503).returnResult(String.class);
            assertThat(respuesta.getResponseHeaders().getFirst("X-Request-Id")).isNotBlank();
        } finally {
            EntornoGateway.SERVICIOS.de("notification-service").start();
        }
    }

    @Test
    @DisplayName("CA-0a-13 la respuesta trae X-Request-Id y el mismo id llega al servicio")
    void requestIdSePropaga() {
        var respuesta = cliente.get().uri("/api/v1/categorias").exchange().expectStatus().isOk().returnResult(String.class);

        String requestId = respuesta.getResponseHeaders().getFirst("X-Request-Id");
        assertThat(requestId).isNotBlank();
        assertThat(java.util.UUID.fromString(requestId)).isNotNull();
        List<LoggedRequest> recibidas = EntornoGateway.SERVICIOS.de("catalog-service").findAll(anyRequestedFor(anyUrl()));
        assertThat(recibidas.get(0).getHeader("X-Request-Id")).isEqualTo(requestId);
    }

    @Test
    @DisplayName("CA-0a-13 un X-Request-Id enviado por el cliente se reemplaza por uno generado por el gateway")
    void requestIdDelClienteSeReemplaza() {
        var respuesta = cliente.get().uri("/api/v1/categorias").header("X-Request-Id", "id-inyectado-por-el-cliente")
                .exchange().expectStatus().isOk().returnResult(String.class);

        assertThat(respuesta.getResponseHeaders().getFirst("X-Request-Id")).isNotEqualTo("id-inyectado-por-el-cliente");
    }

    @Test
    @DisplayName("CA-0a-13 X-Request-Id también sale en las respuestas de error (404 interno y 404 sin ruta)")
    void requestIdEnErrores() {
        for (String ruta : List.of("/internal/x", "/api/v1/no-existe")) {
            var respuesta = cliente.get().uri(ruta).exchange().expectStatus().isNotFound().returnResult(String.class);
            assertThat(respuesta.getResponseHeaders().getFirst("X-Request-Id")).as(ruta).isNotBlank();
        }
    }

    @Test
    @DisplayName("CA-0a-13 el requestId del cuerpo de error coincide con la cabecera X-Request-Id")
    void requestIdEnElCuerpoDelError() {
        var resultado = cliente.get().uri("/internal/x").exchange().expectStatus().isNotFound()
                .returnResult(String.class);
        String cabecera = resultado.getResponseHeaders().getFirst("X-Request-Id");
        String cuerpo = resultado.getResponseBody().blockFirst();

        assertThat(cabecera).isNotBlank();
        assertThat(cuerpo).contains("\"requestId\":\"" + cabecera + "\"");
    }

    @Test
    @DisplayName("FallbackController responde 503 con el mensaje de servicio no disponible")
    void fallback() {
        cliente.get().uri("/fallback/servicio-no-disponible").exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.mensaje").isEqualTo("Servicio no disponible, inténtalo en un momento");
    }

    @Test
    @DisplayName("CA-0a-17 el selector de Swagger lista los 7 servicios")
    void swaggerAgregado() {
        cliente.get().uri("/v3/api-docs/swagger-config").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.urls.length()").isEqualTo(7)
                .jsonPath("$.urls[?(@.name=='auth-service')]").exists()
                .jsonPath("$.urls[?(@.name=='catalog-service')]").exists()
                .jsonPath("$.urls[?(@.name=='order-service')]").exists()
                .jsonPath("$.urls[?(@.name=='payment-service')]").exists()
                .jsonPath("$.urls[?(@.name=='delivery-service')]").exists()
                .jsonPath("$.urls[?(@.name=='notification-service')]").exists()
                .jsonPath("$.urls[?(@.name=='settings-service')]").exists();
    }

    @Test
    @DisplayName("CA-0a-17 la documentación de cada servicio se alcanza a través del gateway")
    void documentacionPorServicio() {
        cliente.get().uri("/catalog-service/v3/api-docs").exchange().expectStatus().isOk();

        List<LoggedRequest> recibidas = EntornoGateway.SERVICIOS.de("catalog-service")
                .findAll(getRequestedFor(com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/v3/api-docs")));
        assertThat(recibidas).hasSize(1);
    }
}
