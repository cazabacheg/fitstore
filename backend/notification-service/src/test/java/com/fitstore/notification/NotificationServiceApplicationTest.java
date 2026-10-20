package com.fitstore.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.notification.pruebas.PostgresInicializador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

// CA-0a-01: el módulo compila y arranca (con seguridad activa, Flyway y validación del esquema)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "sin-config-server"})
@ContextConfiguration(initializers = PostgresInicializador.class)
class NotificationServiceApplicationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("CA-0a-01 el servicio notification arranca y /actuator/health responde UP")
    void arrancaYResponde() throws Exception {
        var respuesta = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andReturn().getResponse();
        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(respuesta.getContentAsString()).contains("UP");
    }

    @Test
    @DisplayName("CA-0a-01 una ruta de negocio sin token queda cerrada (seguridad base, I3)")
    void rutaDeNegocioCerrada() throws Exception {
        var respuesta = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/notificaciones"))
                .andReturn().getResponse();
        assertThat(respuesta.getStatus()).isEqualTo(401);
    }
}
