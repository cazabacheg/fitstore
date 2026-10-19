package com.fitstore.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

// CA-0a-01: el módulo compila y arranca (base mínima; 0a.2 completa el servicio)
@SpringBootTest
@AutoConfigureMockMvc
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
}
