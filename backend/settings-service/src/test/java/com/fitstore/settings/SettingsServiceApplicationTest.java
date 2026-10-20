package com.fitstore.settings;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.settings.pruebas.PostgresDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

// CA-0a-01: el módulo compila y arranca. Ahora con Flyway y Hibernate validate, necesita PostgreSQL (Testcontainers)
@SpringBootTest
@AutoConfigureMockMvc
class SettingsServiceApplicationTest {

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        PostgresDePrueba.registrar(registro);
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("CA-0a-01 el servicio settings arranca y /actuator/health responde UP")
    void arrancaYResponde() throws Exception {
        var respuesta = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andReturn().getResponse();
        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(respuesta.getContentAsString()).contains("UP");
    }
}
