package com.fitstore.payment.service;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.ResultActions;

/** Un reintento con la misma Idempotency-Key devuelve el resultado original sin cobrar otra vez (CA-5-17). */
class IdempotenciaCobroTest extends PagoApiBase {

    private final UUID clienta = UUID.randomUUID();

    private ResultActions cobrar(String codigo, String clave, String cuerpo) throws Exception {
        return mvc.perform(post("/api/v1/pagos/" + codigo + "/cargos").with(Sesiones.cliente(clienta))
                .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static final String MERCADO_PAGO = "{\"proveedor\":\"MERCADO_PAGO\",\"medio\":\"TARJETA\",\"token\":\"tok_brick_abc123\"}";

    @Test
    @DisplayName("CA-5-17 repetir un cobro aprobado con la misma clave devuelve 200 sin un nuevo cargo en la pasarela")
    void repiteUnAprobado() throws Exception {
        WireMockPrueba.servidor().stubFor(crearPagoMp().willReturn(respuesta(201, pagoMp(701, "approved", "accredited"))));
        IntencionPago i = intencion(clienta);
        String clave = UUID.randomUUID().toString();

        cobrar(i.getCodigoPedido(), clave, MERCADO_PAGO).andExpect(status().isOk());
        cobrar(i.getCodigoPedido(), clave, MERCADO_PAGO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("APROBADA"));

        WireMockPrueba.servidor().verify(1, postRequestedFor(urlEqualTo("/v1/payments")));
        assertThat(contarTransacciones(i.getId(), "APROBADA")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transaccion WHERE intencion_id = ?", Integer.class, i.getId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-17 repetir un cobro rechazado con la misma clave devuelve el mismo 402 sin llamar a la pasarela")
    void repiteUnRechazado() throws Exception {
        WireMockPrueba.servidor().stubFor(crearPagoMp()
                .willReturn(respuesta(201, pagoMp(702, "rejected", "cc_rejected_insufficient_amount"))));
        IntencionPago i = intencion(clienta);
        String clave = UUID.randomUUID().toString();

        cobrar(i.getCodigoPedido(), clave, MERCADO_PAGO).andExpect(status().isPaymentRequired());
        cobrar(i.getCodigoPedido(), clave, MERCADO_PAGO)
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.mensaje").value("Fondos insuficientes"));

        WireMockPrueba.servidor().verify(1, postRequestedFor(urlEqualTo("/v1/payments")));
    }

    @Test
    @DisplayName("CA-5-17 repetir un cobro en curso con la misma clave devuelve 202 sin un nuevo cargo")
    void repiteUnEnCurso() throws Exception {
        IntencionPago i = intencion(clienta);
        String clave = UUID.randomUUID().toString();
        String cuerpo = "{\"proveedor\":\"SIMULADO\",\"medio\":\"TARJETA\",\"token\":\"sim_0119\"}";

        cobrar(i.getCodigoPedido(), clave, cuerpo).andExpect(status().isAccepted());
        cobrar(i.getCodigoPedido(), clave, cuerpo).andExpect(status().isAccepted());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM transaccion WHERE intencion_id = ?", Integer.class, i.getId()))
                .isEqualTo(1);
    }
}
