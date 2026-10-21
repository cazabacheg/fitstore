package com.fitstore.payment.provider;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.Sesiones;
import com.fitstore.payment.pruebas.WireMockPrueba;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/** Si la pasarela falla repetidamente el circuito se abre y la página lo muestra como no disponible (CA-5-23). */
@TestPropertySource(properties = {
    "fitstore.pagos.circuito.ventana=2",
    "fitstore.pagos.circuito.minimo-llamadas=2",
    "fitstore.pagos.circuito.espera=60s"})
class CircuitoAbiertoTest extends PagoApiBase {

    private final UUID clienta = UUID.randomUUID();

    private static final String MERCADO_PAGO = "{\"proveedor\":\"MERCADO_PAGO\",\"medio\":\"TARJETA\",\"token\":\"tok_brick_abc123\"}";

    private void cobrarMp(IntencionPago i) throws Exception {
        mvc.perform(post("/api/v1/pagos/" + i.getCodigoPedido() + "/cargos").with(Sesiones.cliente(clienta))
                .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                .content(MERCADO_PAGO));
    }

    @Test
    @DisplayName("CA-5-23 tras fallos repetidos el medio sale \"No disponible por el momento\" y el cobro responde 503")
    void circuitoAbierto() throws Exception {
        WireMockPrueba.servidor().stubFor(crearPagoMp().willReturn(respuesta(503, "{}")));
        cobrarMp(intencion(clienta));
        cobrarMp(intencion(clienta));
        IntencionPago tercera = intencion(clienta);

        mvc.perform(get("/api/v1/pagos/" + tercera.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='MERCADO_PAGO')].disponible").value(hasItem(false)))
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='MERCADO_PAGO')].motivoNoDisponible")
                        .value(hasItem("No disponible por el momento; elige otro medio de pago")))
                .andExpect(jsonPath("$.data.medios[?(@.proveedor=='SIMULADO')].disponible").value(hasItem(true)));

        mvc.perform(post("/api/v1/pagos/" + tercera.getCodigoPedido() + "/cargos").with(Sesiones.cliente(clienta))
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                        .content(MERCADO_PAGO))
                .andExpect(status().isServiceUnavailable());
    }
}
