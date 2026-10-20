package com.fitstore.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.delivery.pruebas.PostgresPrueba;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Guarda el OpenAPI que genera el servicio en {@code target/openapi.json} para {@code scripts/contrato.sh} (SPEC-0a I8). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
class ContratoOpenApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("CA-0a-22 GET /v3/api-docs genera el OpenAPI 3.0 del servicio y lo guarda en target/openapi.json")
    void guardaElOpenApiGenerado() throws Exception {
        var respuesta = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs")).andReturn().getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(200);
        String cuerpo = respuesta.getContentAsString(StandardCharsets.UTF_8);
        assertThat(cuerpo).contains("\"openapi\":\"3.0");
        Path destino = Path.of("target", "openapi.json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, cuerpo, StandardCharsets.UTF_8);
        assertThat(destino).exists();
    }
}
