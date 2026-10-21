package com.fitstore.payment.service;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
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

/** La pasarela no responde a tiempo (el timeout se reduce a 1 s en la prueba, en producción son 10 s): CA-5-18. */
@TestPropertySource(properties = "fitstore.pagos.mercadopago.timeout=1s")
class TimeoutCobroTest extends PagoApiBase {

    private final UUID clienta = UUID.randomUUID();

    @Test
    @DisplayName("CA-5-18 sin respuesta a tiempo responde 202, no reintenta el cargo y la página lo ve como procesando")
    void sinRespuesta() throws Exception {
        WireMockPrueba.servidor().stubFor(crearPagoMp().willReturn(
                respuesta(201, pagoMp(901, "approved", "accredited")).withFixedDelay(3000)));
        IntencionPago i = intencion(clienta);

        mvc.perform(post("/api/v1/pagos/" + i.getCodigoPedido() + "/cargos").with(Sesiones.cliente(clienta))
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedor\":\"MERCADO_PAGO\",\"medio\":\"TARJETA\",\"token\":\"tok_brick_abc123\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").value("Estamos confirmando tu pago"))
                .andExpect(jsonPath("$.data.estado").value("PROCESANDO"));

        // WireMock anota el pedido cuando termina de responder (tras la demora): se espera a que aparezca
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(6)).untilAsserted(
                () -> WireMockPrueba.servidor().verify(1, postRequestedFor(urlEqualTo("/v1/payments"))));
        assertThat(contarTransacciones(i.getId(), "PROCESANDO")).isEqualTo(1);
        assertThat(estadoIntencion(i.getId())).isEqualTo("PENDIENTE");

        mvc.perform(get("/api/v1/pagos/" + i.getCodigoPedido()).with(Sesiones.cliente(clienta)))
                .andExpect(jsonPath("$.data.procesando").value(true));
    }
}
