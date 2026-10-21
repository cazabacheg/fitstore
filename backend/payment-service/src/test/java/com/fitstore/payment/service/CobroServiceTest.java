package com.fitstore.payment.service;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.Sesiones;
import com.fitstore.payment.pruebas.WireMockPrueba;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Cobro del pedido con el monto de la intención (T3): aprobado, rechazado, en curso y las validaciones previas. */
class CobroServiceTest extends PagoApiBase {

    private final UUID clienta = UUID.randomUUID();

    private ResultActions cobrar(String codigo, UUID cliente, String clave, String cuerpo) throws Exception {
        return mvc.perform(post("/api/v1/pagos/" + codigo + "/cargos").with(Sesiones.cliente(cliente))
                .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String simulado(String token) {
        return "{\"proveedor\":\"SIMULADO\",\"medio\":\"TARJETA\",\"token\":\"" + token + "\"}";
    }

    @Test
    @DisplayName("CA-5-25 con el simulado, sim_1111 aprueba: transacción APROBADA, intención PAGADA y evento PagoConfirmado")
    void simuladoAprueba() throws Exception {
        IntencionPago i = intencion(clienta);

        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_1111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("¡Pago aprobado!"))
                .andExpect(jsonPath("$.data.estado").value("APROBADA"))
                .andExpect(jsonPath("$.data.codigoPedido").value(i.getCodigoPedido()))
                .andExpect(jsonPath("$.data.monto").value(119.80))
                .andExpect(jsonPath("$.data.proveedor").value("SIMULADO"));

        assertThat(contarTransacciones(i.getId(), "APROBADA")).isEqualTo(1);
        assertThat(estadoIntencion(i.getId())).isEqualTo("PAGADA");
        assertThat(eventosEnOutbox(i.getPedidoId(), "PagoConfirmado")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-25 sim_0002 se rechaza con 402 y motivo legible, publica PagoRechazado y deja reintentar")
    void simuladoRechaza() throws Exception {
        IntencionPago i = intencion(clienta);

        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_0002"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("PAGO_RECHAZADO"))
                .andExpect(jsonPath("$.mensaje").value("Fondos insuficientes"));

        assertThat(contarTransacciones(i.getId(), "RECHAZADA")).isEqualTo(1);
        assertThat(estadoIntencion(i.getId())).isEqualTo("PENDIENTE");
        assertThat(eventosEnOutbox(i.getPedidoId(), "PagoRechazado")).isEqualTo(1);

        // se puede reintentar con otra tarjeta mientras el pedido no venza
        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_1111"))
                .andExpect(status().isOk());
        assertThat(estadoIntencion(i.getId())).isEqualTo("PAGADA");
    }

    @Test
    @DisplayName("CA-5-25 sim_0119 queda PROCESANDO con 202 \"Estamos confirmando tu pago\"")
    void simuladoProcesando() throws Exception {
        IntencionPago i = intencion(clienta);

        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_0119"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.mensaje").value("Estamos confirmando tu pago"))
                .andExpect(jsonPath("$.data.estado").value("PROCESANDO"));

        assertThat(contarTransacciones(i.getId(), "PROCESANDO")).isEqualTo(1);
        assertThat(estadoIntencion(i.getId())).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("CA-5-15 con Mercado Pago cobra el monto de la intención con la misma clave como X-Idempotency-Key")
    void mercadoPagoCobraElMontoDeLaIntencion() throws Exception {
        WireMockPrueba.servidor().stubFor(crearPagoMp().willReturn(respuesta(201, pagoMp(555, "approved", "accredited"))));
        IntencionPago i = intencion(clienta);
        String clave = UUID.randomUUID().toString();

        cobrar(i.getCodigoPedido(), clienta, clave, """
                {"proveedor":"MERCADO_PAGO","medio":"TARJETA","token":"tok_brick_abc123","email":"ana@correo.pe",
                 "datosAdicionales":{"payment_method_id":"visa","installments":1,"issuer_id":"310"}}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.proveedor").value("MERCADO_PAGO"))
                .andExpect(jsonPath("$.data.descripcionMedio").value("Visa ****1111"));

        WireMockPrueba.servidor().verify(postRequestedFor(urlEqualTo("/v1/payments"))
                .withHeader("X-Idempotency-Key", equalTo(clave))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.equalToJson(
                        "{\"transaction_amount\":119.80}", true, true)));
        assertThat(estadoIntencion(i.getId())).isEqualTo("PAGADA");
    }

    @Test
    @DisplayName("CA-5-15 un rechazo de Mercado Pago devuelve 402 con el motivo traducido")
    void mercadoPagoRechaza() throws Exception {
        WireMockPrueba.servidor().stubFor(crearPagoMp()
                .willReturn(respuesta(201, pagoMp(556, "rejected", "cc_rejected_insufficient_amount"))));
        IntencionPago i = intencion(clienta);

        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), """
                {"proveedor":"MERCADO_PAGO","medio":"TARJETA","token":"tok_brick_abc123"}""")
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.mensaje").value("Fondos insuficientes"));
    }

    @Test
    @DisplayName("CA-5-08 si el plazo venció responde 409 PAGO_VENCIDO y no cobra")
    void vencido() throws Exception {
        IntencionPago i = intencion(clienta, "119.80", Instant.now().minus(1, ChronoUnit.MINUTES));

        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_1111"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PAGO_VENCIDO"));
        assertThat(contarTransacciones(i.getId(), "APROBADA")).isZero();
    }

    @Test
    @DisplayName("CA-5-16 un pedido ya pagado responde 409 YA_PAGADO")
    void yaPagado() throws Exception {
        IntencionPago i = intencion(clienta);
        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_1111")).andExpect(status().isOk());

        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_1111"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("YA_PAGADO"))
                .andExpect(jsonPath("$.mensaje").value("Este pedido ya fue pagado"));
        assertThat(contarTransacciones(i.getId(), "APROBADA")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-16 un pedido anulado responde 409 PAGO_ANULADO")
    void anulado() throws Exception {
        IntencionPago i = intencion(clienta);
        i.setEstado(EstadoIntencion.ANULADA);
        intenciones.saveAndFlush(i);

        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_1111"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PAGO_ANULADO"));
    }

    @Test
    @DisplayName("CA-5-16 con un intento anterior todavía en curso no se acepta otro cobro (evita cobrar dos veces)")
    void conUnIntentoEnCurso() throws Exception {
        IntencionPago i = intencion(clienta);
        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_0119")).andExpect(status().isAccepted());

        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), simulado("sim_1111"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PAGO_EN_CURSO"));
        assertThat(contarTransacciones(i.getId(), "APROBADA")).isZero();
    }

    @Test
    @DisplayName("CA-5-07 cobrar un pedido ajeno responde 404")
    void ajeno() throws Exception {
        IntencionPago i = intencion(clienta);

        cobrar(i.getCodigoPedido(), UUID.randomUUID(), UUID.randomUUID().toString(), simulado("sim_1111"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CA-5-15 un proveedor sin credenciales o sin habilitar en la tienda responde 400")
    void medioNoHabilitado() throws Exception {
        org.mockito.Mockito.when(tienda.metodosPago())
                .thenReturn(java.util.Optional.of(java.util.List.of("CULQI")));
        IntencionPago i = intencion(clienta);

        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), """
                {"proveedor":"MERCADO_PAGO","medio":"TARJETA","token":"tok_brick_abc123"}""")
                .andExpect(status().isBadRequest());
        cobrar(i.getCodigoPedido(), clienta, UUID.randomUUID().toString(), """
                {"proveedor":"CULQI","medio":"TARJETA","token":"tkn_test_123456"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CA-5-17 sin la cabecera Idempotency-Key responde 400")
    void sinClave() throws Exception {
        IntencionPago i = intencion(clienta);

        mvc.perform(post("/api/v1/pagos/" + i.getCodigoPedido() + "/cargos").with(Sesiones.cliente(clienta))
                        .contentType(MediaType.APPLICATION_JSON).content(simulado("sim_1111")))
                .andExpect(status().isBadRequest());
    }
}
