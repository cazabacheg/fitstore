package com.fitstore.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Formato de ApiResponse (K4), ApiError (K5) y página (K6). */
class ApiRespuestasTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("K4 ApiResponse.ok serializa success, mensaje y data")
    void apiResponseOk() {
        JsonNode nodo = json.valueToTree(ApiResponse.ok(Map.of("id", 7)));

        assertThat(nodo.get("success").asBoolean()).isTrue();
        assertThat(nodo.get("mensaje").asString()).isEqualTo("Operación exitosa");
        assertThat(nodo.get("data").get("id").asInt()).isEqualTo(7);
    }

    @Test
    @DisplayName("K4 ApiResponse.ok con mensaje propio y sin datos")
    void apiResponseConMensaje() {
        ApiResponse<Void> respuesta = ApiResponse.ok("Sesión cerrada");

        assertThat(respuesta.success()).isTrue();
        assertThat(respuesta.mensaje()).isEqualTo("Sesión cerrada");
        assertThat(respuesta.data()).isNull();
    }

    @Test
    @DisplayName("K5 ApiError lleva success false, mensaje, codigo, errores, requestId y detalles")
    void apiErrorCompleto() {
        ApiError error = ApiError.de("Revisa los datos ingresados", "VALIDACION")
                .conErrores(List.of(new ErrorCampo("dni", "El DNI debe tener 8 dígitos")))
                .conRequestId("req-1")
                .conDetalles(Map.of("faltantes", List.of(1)));

        JsonNode nodo = json.valueToTree(error);

        assertThat(nodo.get("success").asBoolean()).isFalse();
        assertThat(nodo.get("mensaje").asString()).isEqualTo("Revisa los datos ingresados");
        assertThat(nodo.get("codigo").asString()).isEqualTo("VALIDACION");
        assertThat(nodo.get("errores").get(0).get("campo").asString()).isEqualTo("dni");
        assertThat(nodo.get("errores").get(0).get("mensaje").asString()).isEqualTo("El DNI debe tener 8 dígitos");
        assertThat(nodo.get("requestId").asString()).isEqualTo("req-1");
        assertThat(nodo.get("detalles").get("faltantes").get(0).asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("K5 ApiError omite errores, requestId y detalles cuando no existen")
    void apiErrorMinimo() {
        JsonNode nodo = json.valueToTree(ApiError.de("No encontramos lo que buscas", "NO_ENCONTRADO"));

        assertThat(nodo.has("errores")).isFalse();
        assertThat(nodo.has("requestId")).isFalse();
        assertThat(nodo.has("detalles")).isFalse();
        assertThat(nodo.get("codigo").asString()).isEqualTo("NO_ENCONTRADO");
    }

    @Test
    @DisplayName("K6 PaginaResponse expone contenido, pagina, tamano, totalElementos y totalPaginas")
    void pagina() {
        PaginaResponse<String> pagina = new PaginaResponse<>(List.of("a", "b"), 0, 20, 2L, 1);

        JsonNode nodo = json.valueToTree(pagina);

        assertThat(nodo.get("contenido")).hasSize(2);
        assertThat(nodo.get("pagina").asInt()).isZero();
        assertThat(nodo.get("tamano").asInt()).isEqualTo(20);
        assertThat(nodo.get("totalElementos").asLong()).isEqualTo(2L);
        assertThat(nodo.get("totalPaginas").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("K6 PaginaResponse.de convierte una Page de Spring Data")
    void paginaDesdeSpringData() {
        var page = new org.springframework.data.domain.PageImpl<>(
                List.of("x"), org.springframework.data.domain.PageRequest.of(2, 1), 5);

        PaginaResponse<String> pagina = PaginaResponse.de(page);

        assertThat(pagina.contenido()).containsExactly("x");
        assertThat(pagina.pagina()).isEqualTo(2);
        assertThat(pagina.tamano()).isEqualTo(1);
        assertThat(pagina.totalElementos()).isEqualTo(5L);
        assertThat(pagina.totalPaginas()).isEqualTo(5);
    }

    @Test
    @DisplayName("Las excepciones comunes llevan estado y codigo estables")
    void excepciones() {
        var noEncontrado = new RecursoNoEncontradoException("No encontramos el producto");
        var conflicto = new ConflictoNegocioException("CORREO_DUPLICADO", "Ese correo ya está registrado");

        assertThat(noEncontrado.estado().value()).isEqualTo(404);
        assertThat(noEncontrado.codigo()).isEqualTo("NO_ENCONTRADO");
        assertThat(conflicto.estado().value()).isEqualTo(409);
        assertThat(conflicto.codigo()).isEqualTo("CORREO_DUPLICADO");
        assertThat(conflicto.getMessage()).isEqualTo("Ese correo ya está registrado");
    }
}
