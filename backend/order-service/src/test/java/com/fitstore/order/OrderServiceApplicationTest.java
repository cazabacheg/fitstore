package com.fitstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.order.pruebas.EntornoPrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

// CA-0a-01: el módulo compila y arranca (con seguridad base, Flyway y validate sobre PostgreSQL real)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = EntornoPrueba.class)
class OrderServiceApplicationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("CA-0a-01 el servicio order arranca y /actuator/health responde UP")
    void arrancaYResponde() throws Exception {
        var respuesta = mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health")).andReturn().getResponse();
        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(respuesta.getContentAsString()).contains("UP");
    }

    @Test
    @DisplayName("CA-0a-01 la seguridad base cierra todo salvo health y la documentación (I3)")
    void seguridadBase() throws Exception {
        var cerrada = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/pedidos")).andReturn().getResponse();
        assertThat(cerrada.getStatus()).isEqualTo(401);

        var documentacion = mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs")).andReturn().getResponse();
        assertThat(documentacion.getStatus()).isEqualTo(200);
    }
}
