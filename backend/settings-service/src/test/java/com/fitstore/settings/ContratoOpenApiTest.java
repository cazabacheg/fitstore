package com.fitstore.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.settings.pruebas.PostgresDePrueba;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * SPEC-0a I8: guarda el OpenAPI que genera el servicio en {@code target/openapi.json} para que
 * {@code scripts/contrato.sh} lo compare con {@code contracts/openapi/settings-service.yaml} (oasdiff).
 */
@SpringBootTest(properties = {
    "fitstore.eventos.servicio=settings",
    "fitstore.outbox.habilitado=true",
    "fitstore.outbox.intervalo-ms=3600000",
    "springdoc.api-docs.version=openapi_3_0"
})
@AutoConfigureMockMvc
class ContratoOpenApiTest {

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        PostgresDePrueba.registrar(registro);
    }

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("CA-0a-22 guarda el OpenAPI generado en target/openapi.json")
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
