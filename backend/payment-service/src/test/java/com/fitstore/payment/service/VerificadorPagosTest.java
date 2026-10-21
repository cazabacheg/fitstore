package com.fitstore.payment.service;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.WireMockPrueba;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Verificador de pagos en curso (cada 2 min): consulta la pasarela y aprueba o rechaza (CA-5-22). */
class VerificadorPagosTest extends PagoApiBase {

    @Autowired VerificadorPagosService verificador;

    private final UUID clienta = UUID.randomUUID();

    /** Las pruebas comparten base de datos: lo que dejaron en curso otras pruebas no debe entrar a esta pasada. */
    @org.junit.jupiter.api.BeforeEach
    void aislar() {
        jdbc.update("UPDATE transaccion SET estado = 'ERROR' WHERE estado = 'PROCESANDO'");
    }

    private static long idUnico() {
        return ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
    }

    @Test
    @DisplayName("CA-5-22 una transacción PROCESANDO de más de 2 minutos se consulta a la pasarela y se aprueba")
    void apruebaLoPendiente() {
        long idPago = idUnico();
        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago).willReturn(respuesta(200, pagoMp(idPago, "approved", "accredited"))));
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, String.valueOf(idPago), 5);

        verificador.verificar();

        assertThat(estadoTransaccion(t.getId())).isEqualTo("APROBADA");
        assertThat(estadoIntencion(i.getId())).isEqualTo("PAGADA");
        assertThat(eventosEnOutbox(i.getPedidoId(), "PagoConfirmado")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-22 si la pasarela informa un rechazo la transacción queda RECHAZADA y la intención sigue pendiente")
    void rechazaLoPendiente() {
        long idPago = idUnico();
        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago)
                .willReturn(respuesta(200, pagoMp(idPago, "rejected", "cc_rejected_insufficient_amount"))));
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, String.valueOf(idPago), 5);

        verificador.verificar();

        assertThat(estadoTransaccion(t.getId())).isEqualTo("RECHAZADA");
        assertThat(estadoIntencion(i.getId())).isEqualTo("PENDIENTE");
        assertThat(eventosEnOutbox(i.getPedidoId(), "PagoRechazado")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-22 una transacción de menos de 2 minutos no se consulta todavía")
    void respetaLaAntiguedad() {
        long idPago = idUnico();
        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago).willReturn(respuesta(200, pagoMp(idPago, "approved", "accredited"))));
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, String.valueOf(idPago), 0);

        verificador.verificar();

        assertThat(estadoTransaccion(t.getId())).isEqualTo("PROCESANDO");
        WireMockPrueba.servidor().verify(0, getRequestedFor(urlPathEqualTo("/v1/payments/" + idPago)));
    }

    @Test
    @DisplayName("CA-5-22 si la pasarela sigue sin responder la transacción queda PROCESANDO para la siguiente pasada")
    void pasarelaCaida() {
        long idPago = idUnico();
        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago).willReturn(respuesta(503, "{}")));
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, String.valueOf(idPago), 5);

        verificador.verificar();

        assertThat(estadoTransaccion(t.getId())).isEqualTo("PROCESANDO");
    }

    @Test
    @DisplayName("CA-5-22 el cobro simulado en curso (tarjeta ...0119) se aprueba en la siguiente pasada")
    void simuladoEnCurso() {
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.SIMULADO, "sim-pendiente-" + UUID.randomUUID(), 5);

        verificador.verificar();

        assertThat(estadoTransaccion(t.getId())).isEqualTo("APROBADA");
        assertThat(estadoIntencion(i.getId())).isEqualTo("PAGADA");
    }

    @Test
    @DisplayName("CA-5-22 un cargo sin id de la pasarela se busca por su referencia")
    void buscaPorReferencia() {
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, null, 5);
        long idPago = idUnico();
        WireMockPrueba.servidor().stubFor(get(urlPathEqualTo("/v1/payments/search")).willReturn(respuesta(200,
                "{\"results\":[" + pagoMp(idPago, "approved", "accredited") + "]}")));

        verificador.verificar();

        assertThat(estadoTransaccion(t.getId())).isEqualTo("APROBADA");
        assertThat(jdbc.queryForObject("SELECT id_proveedor FROM transaccion WHERE id = ?", String.class, t.getId()))
                .isEqualTo(String.valueOf(idPago));
        WireMockPrueba.servidor().verify(getRequestedFor(urlPathEqualTo("/v1/payments/search"))
                .withQueryParam("external_reference", com.github.tomakehurst.wiremock.client.WireMock.equalTo(t.getId().toString())));
    }

    @Test
    @DisplayName("CA-5-22 un cargo sin id y sin rastro en la pasarela pasa a ERROR cuando ya pasó mucho tiempo")
    void sinRastroPasaAError() {
        WireMockPrueba.servidor().stubFor(get(urlPathEqualTo("/v1/payments/search")).willReturn(respuesta(200, "{\"results\":[]}")));
        IntencionPago reciente = intencion(clienta);
        Transaccion tReciente = transaccionProcesando(reciente, Proveedor.MERCADO_PAGO, null, 5);
        IntencionPago antigua = intencion(clienta);
        Transaccion tAntigua = transaccionProcesando(antigua, Proveedor.MERCADO_PAGO, null, 30);

        verificador.verificar();

        assertThat(estadoTransaccion(tReciente.getId())).isEqualTo("PROCESANDO");
        assertThat(estadoTransaccion(tAntigua.getId())).isEqualTo("ERROR");
        assertThat(estadoIntencion(antigua.getId())).isEqualTo("PENDIENTE");
    }
}
