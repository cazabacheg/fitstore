package com.fitstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.catalog.pruebas.PostgresPrueba;
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

/**
 * Guarda el OpenAPI que genera el servicio en {@code target/openapi.json} para que {@code scripts/contrato.sh}
 * lo compare con {@code contracts/openapi/catalog-service.yaml} (SPEC-0a I8). Todavía casi vacío: los endpoints llegan en la spec 2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
class ContratoOpenApiTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("CA-0a-22 guarda GET /v3/api-docs en target/openapi.json como OpenAPI 3.0")
    void guardaElOpenApi() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Path destino = Path.of("target", "openapi.json");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, json);

        assertThat(json).contains("\"openapi\":\"3.0");
        assertThat(Files.size(destino)).isPositive();
    }
}
