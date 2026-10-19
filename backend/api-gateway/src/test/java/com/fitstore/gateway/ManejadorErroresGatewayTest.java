package com.fitstore.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.gateway.web.ManejadorErroresGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** CA-0a-12 y K5: cada tipo de error del gateway sale con el formato ApiError. */
class ManejadorErroresGatewayTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final ManejadorErroresGateway manejador = new ManejadorErroresGateway(json);

    private MockServerWebExchange manejar(Throwable error) {
        MockServerWebExchange intercambio = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/algo").header("X-Request-Id", "id-123").build());
        manejador.handle(intercambio, error).block();
        return intercambio;
    }

    private JsonNode cuerpo(MockServerWebExchange intercambio) throws Exception {
        return json.readTree(intercambio.getResponse().getBodyAsString().block());
    }

    @Test
    @DisplayName("CA-0a-12 un 4xx genérico (p. ej. 405) responde con el mismo estado y código VALIDACION")
    void cuatroXXGenerico() throws Exception {
        MockServerWebExchange intercambio = manejar(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED));

        assertThat(intercambio.getResponse().getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        JsonNode cuerpo = cuerpo(intercambio);
        assertThat(cuerpo.get("success").asBoolean()).isFalse();
        assertThat(cuerpo.get("codigo").asString()).isEqualTo("VALIDACION");
        assertThat(cuerpo.get("mensaje").asString()).isEqualTo("Revisa los datos ingresados");
        assertThat(cuerpo.get("requestId").asString()).isEqualTo("id-123");
    }

    @Test
    @DisplayName("CA-0a-12 un error inesperado responde 500 ERROR_INTERNO sin exponer el detalle")
    void errorInesperado() throws Exception {
        MockServerWebExchange intercambio = manejar(new IllegalStateException("detalle interno secreto"));

        assertThat(intercambio.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        JsonNode cuerpo = cuerpo(intercambio);
        assertThat(cuerpo.get("success").asBoolean()).isFalse();
        assertThat(cuerpo.get("codigo").asString()).isEqualTo("ERROR_INTERNO");
        assertThat(cuerpo.get("mensaje").asString()).doesNotContain("secreto");
    }

    @Test
    @DisplayName("CA-0a-12 una causa de conexión rechazada responde 503 SERVICIO_NO_DISPONIBLE")
    void conexionRechazada() throws Exception {
        MockServerWebExchange intercambio = manejar(new RuntimeException("falló", new java.net.ConnectException("rechazada")));

        assertThat(intercambio.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(cuerpo(intercambio).get("codigo").asString()).isEqualTo("SERVICIO_NO_DISPONIBLE");
    }

    @Test
    @DisplayName("CA-0a-12 un 404 sin ruta responde NO_ENCONTRADO")
    void sinRuta() throws Exception {
        MockServerWebExchange intercambio = manejar(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThat(intercambio.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(cuerpo(intercambio).get("codigo").asString()).isEqualTo("NO_ENCONTRADO");
    }
}
