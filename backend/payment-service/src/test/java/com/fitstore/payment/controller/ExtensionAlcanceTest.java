package com.fitstore.payment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Las rutas del contrato que son extensión de alcance (reembolsos del backoffice y el webhook de Culqi) existen con la
 * misma firma pero responden 501 {@code EXTENSION_DE_ALCANCE}, sin lógica (docs/implementacion/extension-alcance.md).
 */
class ExtensionAlcanceTest extends PagoApiBase {

    @Test
    @DisplayName("el listado de reembolsos del backoffice responde 501 EXTENSION_DE_ALCANCE")
    void listarReembolsos() throws Exception {
        mvc.perform(get("/api/v1/admin/reembolsos").param("estado", "FALLIDO").param("size", "1").with(Sesiones.admin()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("EXTENSION_DE_ALCANCE"));
    }

    @Test
    @DisplayName("reintentar y resolver a mano un reembolso responden 501 EXTENSION_DE_ALCANCE")
    void reintentarYResolver() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/api/v1/admin/reembolsos/" + id + "/reintentar").with(Sesiones.admin()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo").value("EXTENSION_DE_ALCANCE"));
        mvc.perform(patch("/api/v1/admin/reembolsos/" + id + "/resuelto-manual").with(Sesiones.admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nota\":\"Devuelto por transferencia\"}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo").value("EXTENSION_DE_ALCANCE"));
    }

    @Test
    @DisplayName("el webhook de Culqi responde 501 EXTENSION_DE_ALCANCE")
    void webhookCulqi() throws Exception {
        mvc.perform(post("/api/v1/pagos/webhooks/culqi").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.codigo").value("EXTENSION_DE_ALCANCE"));
    }

    @Test
    @DisplayName("los reembolsos del backoffice siguen siendo solo del ADMIN")
    void soloAdmin() throws Exception {
        mvc.perform(get("/api/v1/admin/reembolsos").with(Sesiones.operador())).andExpect(status().isForbidden());
    }
}
