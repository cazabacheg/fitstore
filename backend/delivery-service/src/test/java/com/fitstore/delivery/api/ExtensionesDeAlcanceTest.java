package com.fitstore.delivery.api;

import static com.fitstore.delivery.pruebas.Sesiones.operador;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.delivery.pruebas.PruebaApi;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Los endpoints del contrato que son extensión de alcance existen, pero responden 501 (extension-alcance.md). */
class ExtensionesDeAlcanceTest extends PruebaApi {

    @Test
    @DisplayName("las entregas del día responden 501 EXTENSION_DE_ALCANCE")
    void entregasDelDia() throws Exception {
        mvc.perform(get("/api/v1/admin/entregas/dia").with(operador()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("EXTENSION_DE_ALCANCE"));
    }

    @Test
    @DisplayName("el resumen de entregas responde 501 EXTENSION_DE_ALCANCE")
    void resumen() throws Exception {
        mvc.perform(get("/api/v1/admin/entregas/resumen").with(operador()).param("fecha", "2026-10-19"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo").value("EXTENSION_DE_ALCANCE"));
    }

    @Test
    @DisplayName("la devolución a la tienda responde 501 EXTENSION_DE_ALCANCE")
    void devuelta() throws Exception {
        mvc.perform(patch("/api/v1/admin/entregas/" + UUID.randomUUID() + "/devuelta").with(operador())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"No recogido\",\"version\":0}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo").value("EXTENSION_DE_ALCANCE"));
    }
}
