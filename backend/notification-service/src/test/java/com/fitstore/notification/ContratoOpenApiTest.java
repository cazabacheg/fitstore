package com.fitstore.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.notification.pruebas.PostgresInicializador;
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

/** Guarda el OpenAPI generado en {@code target/openapi.json} para {@code scripts/contrato.sh} (SPEC-0a I8). */
@SpringBootTest(properties = "springdoc.api-docs.version=openapi_3_0")
@AutoConfigureMockMvc
@ActiveProfiles({"test", "sin-config-server"})
@ContextConfiguration(initializers = PostgresInicializador.class)
class ContratoOpenApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("CA-0a-22 guarda GET /v3/api-docs en target/openapi.json (OpenAPI 3.0)")
    void guardaElOpenApiGenerado() throws Exception {
        var respuesta = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs")).andReturn().getResponse();
        assertThat(respuesta.getStatus()).isEqualTo(200);
        String json = respuesta.getContentAsString();
        assertThat(json).contains("\"openapi\":\"3.0");
        Path destino = Path.of("target", "openapi.json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, json);
        assertThat(destino).exists();
    }
}
