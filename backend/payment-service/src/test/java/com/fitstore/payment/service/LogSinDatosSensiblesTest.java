package com.fitstore.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.Sesiones;
import com.fitstore.payment.pruebas.WireMockPrueba;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

/** Ni el token de un solo uso ni las credenciales aparecen en los logs, aunque el cobro falle (CA-5-15, RNF-06). */
@ExtendWith(OutputCaptureExtension.class)
class LogSinDatosSensiblesTest extends PagoApiBase {

    private static final String TOKEN = "tok_brick_SECRETO_9f8e7d6c";

    @Test
    @DisplayName("CA-5-15 los logs de un cobro con Mercado Pago no contienen el token, el access token ni el secreto de webhooks")
    void sinDatosSensibles(CapturedOutput salida) throws Exception {
        WireMockPrueba.servidor().stubFor(crearPagoMp().willReturn(respuesta(201, pagoMp(1001, "approved", "accredited"))));
        UUID clienta = UUID.randomUUID();
        IntencionPago i = intencion(clienta);

        mvc.perform(post("/api/v1/pagos/" + i.getCodigoPedido() + "/cargos").with(Sesiones.cliente(clienta))
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedor\":\"MERCADO_PAGO\",\"medio\":\"TARJETA\",\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isOk());

        assertThat(salida.getAll()).doesNotContain(TOKEN).doesNotContain("TEST-token-de-prueba")
                .doesNotContain("secreto-de-prueba");
    }

    @Test
    @DisplayName("CA-5-15 tampoco quedan en el log cuando la pasarela falla")
    void sinDatosSensiblesConFallo(CapturedOutput salida) throws Exception {
        WireMockPrueba.servidor().stubFor(crearPagoMp().willReturn(respuesta(503, "{}")));
        UUID clienta = UUID.randomUUID();
        IntencionPago i = intencion(clienta);

        mvc.perform(post("/api/v1/pagos/" + i.getCodigoPedido() + "/cargos").with(Sesiones.cliente(clienta))
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedor\":\"MERCADO_PAGO\",\"medio\":\"TARJETA\",\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isAccepted());

        assertThat(salida.getAll()).doesNotContain(TOKEN).doesNotContain("TEST-token-de-prueba");
    }
}
