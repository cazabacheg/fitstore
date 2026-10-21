package com.fitstore.payment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.Sesiones;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/** Con {@code PAGOS_SIMULADO_HABILITADO=false} (el valor por defecto) el pago de prueba no existe (HU-5-08). */
@TestPropertySource(properties = "fitstore.pagos.simulado.habilitado=false")
class SimuladoDeshabilitadoTest extends PagoApiBase {

    private final UUID clienta = UUID.randomUUID();

    @Test
    @DisplayName("CA-5-24 con el simulado deshabilitado no aparece \"Pago de prueba\" en la página")
    void noAparece() throws Exception {
        IntencionPago i = intencion(clienta);

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.simuladoHabilitado").value(false))
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='SIMULADO')]").isEmpty());
    }

    @Test
    @DisplayName("CA-5-24 con el simulado deshabilitado la API rechaza el cobro con 400")
    void laApiLoRechaza() throws Exception {
        IntencionPago i = intencion(clienta);

        mvc.perform(post("/api/v1/pagos/" + i.getCodigoPedido() + "/cargos").with(Sesiones.cliente(clienta))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedor\":\"SIMULADO\",\"medio\":\"TARJETA\",\"token\":\"sim_1111\"}"))
                .andExpect(status().isBadRequest());
        org.assertj.core.api.Assertions.assertThat(contarTransacciones(i.getId(), "APROBADA")).isZero();
    }
}
