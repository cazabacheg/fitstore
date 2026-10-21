package com.fitstore.payment.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.WireMockPrueba;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Webhooks de Mercado Pago: firma, guardado idempotente y confirmación consultando la API (HU-5-07: CA-5-20, CA-5-21). */
class WebhookControllerTest extends PagoApiBase {

    private static final String SECRETO = "secreto-de-prueba";

    private final UUID clienta = UUID.randomUUID();

    /** Firma de Mercado Pago: HMAC-SHA256 de {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;}. */
    private static String firma(String dataId, String idPeticion, String ts, String secreto) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(("id:" + dataId + ";request-id:" + idPeticion + ";ts:" + ts + ";")
                .getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append("%02x".formatted(b));
        }
        return "ts=" + ts + ",v1=" + hex;
    }

    private ResultActions webhook(String cuerpo, String firma, String idPeticion) throws Exception {
        return mvc.perform(post("/api/v1/pagos/webhooks/mercadopago").header("x-signature", firma)
                .header("x-request-id", idPeticion).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private static String cuerpo(long notificacion, String tipo, String idPago) {
        return "{\"id\":" + notificacion + ",\"type\":\"" + tipo + "\",\"action\":\"payment.updated\",\"data\":{\"id\":\"" + idPago + "\"}}";
    }

    private int webhooksGuardados(long notificacion) {
        return jdbc.queryForObject("SELECT count(*) FROM webhook_recibido WHERE proveedor = 'MERCADO_PAGO' AND evento_id_proveedor = ?",
                Integer.class, String.valueOf(notificacion));
    }

    private static long idUnico() {
        return ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
    }

    @Test
    @DisplayName("CA-5-20 un webhook válido se guarda, responde 200 y confirma el pago consultando la API de Mercado Pago")
    void confirmaConsultandoLaApi() throws Exception {
        long idPago = idUnico();
        long notificacion = idUnico();
        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago).willReturn(respuesta(200, pagoMp(idPago, "approved", "accredited"))));
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, String.valueOf(idPago), 0);

        webhook(cuerpo(notificacion, "payment", String.valueOf(idPago)), firma(String.valueOf(idPago), "req-1", "1700000000", SECRETO), "req-1")
                .andExpect(status().isOk());

        assertThat(estadoTransaccion(t.getId())).isEqualTo("APROBADA");
        assertThat(estadoIntencion(i.getId())).isEqualTo("PAGADA");
        assertThat(eventosEnOutbox(i.getPedidoId(), "PagoConfirmado")).isEqualTo(1);
        assertThat(webhooksGuardados(notificacion)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT procesado_en IS NOT NULL FROM webhook_recibido WHERE evento_id_proveedor = ?",
                Boolean.class, String.valueOf(notificacion))).isTrue();
    }

    @Test
    @DisplayName("CA-5-20 un webhook que informa un rechazo deja la transacción RECHAZADA y publica PagoRechazado")
    void rechazo() throws Exception {
        long idPago = idUnico();
        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago)
                .willReturn(respuesta(200, pagoMp(idPago, "rejected", "cc_rejected_insufficient_amount"))));
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, String.valueOf(idPago), 0);

        webhook(cuerpo(idUnico(), "payment", String.valueOf(idPago)), firma(String.valueOf(idPago), "req-2", "1700000001", SECRETO), "req-2")
                .andExpect(status().isOk());

        assertThat(estadoTransaccion(t.getId())).isEqualTo("RECHAZADA");
        assertThat(estadoIntencion(i.getId())).isEqualTo("PENDIENTE");
        assertThat(eventosEnOutbox(i.getPedidoId(), "PagoRechazado")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-20 un webhook repetido (mismo id) no se procesa dos veces")
    void duplicado() throws Exception {
        long idPago = idUnico();
        long notificacion = idUnico();
        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago).willReturn(respuesta(200, pagoMp(idPago, "approved", "accredited"))));
        IntencionPago i = intencion(clienta);
        transaccionProcesando(i, Proveedor.MERCADO_PAGO, String.valueOf(idPago), 0);
        String cuerpo = cuerpo(notificacion, "payment", String.valueOf(idPago));
        String firma = firma(String.valueOf(idPago), "req-3", "1700000002", SECRETO);

        webhook(cuerpo, firma, "req-3").andExpect(status().isOk());
        webhook(cuerpo, firma, "req-3").andExpect(status().isOk());

        assertThat(webhooksGuardados(notificacion)).isEqualTo(1);
        WireMockPrueba.servidor().verify(1, getRequestedFor(urlPathEqualTo("/v1/payments/" + idPago)));
        assertThat(eventosEnOutbox(i.getPedidoId(), "PagoConfirmado")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-20 si la transacción no tiene el id de la pasarela, el aviso la reconoce por su referencia")
    void reconocePorReferencia() throws Exception {
        long idPago = idUnico();
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, null, 0);
        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago).willReturn(respuesta(200, """
                {"id":%d,"status":"approved","status_detail":"accredited","payment_method_id":"visa","external_reference":"%s"}"""
                .formatted(idPago, t.getId()))));

        webhook(cuerpo(idUnico(), "payment", String.valueOf(idPago)), firma(String.valueOf(idPago), "req-4", "1700000003", SECRETO), "req-4")
                .andExpect(status().isOk());

        assertThat(estadoTransaccion(t.getId())).isEqualTo("APROBADA");
        assertThat(jdbc.queryForObject("SELECT id_proveedor FROM transaccion WHERE id = ?", String.class, t.getId()))
                .isEqualTo(String.valueOf(idPago));
    }

    @Test
    @DisplayName("CA-5-20 un aviso que no es de un pago se guarda y responde 200 sin consultar nada")
    void avisoQueNoEsDePago() throws Exception {
        long notificacion = idUnico();

        webhook(cuerpo(notificacion, "merchant_order", "777"), firma("777", "req-5", "1700000004", SECRETO), "req-5")
                .andExpect(status().isOk());

        assertThat(webhooksGuardados(notificacion)).isEqualTo(1);
        WireMockPrueba.servidor().verify(0, getRequestedFor(urlPathEqualTo("/v1/payments/777")));
    }

    @Test
    @DisplayName("CA-5-21 con la firma inválida responde 401 y no se guarda ni se procesa")
    void firmaInvalida() throws Exception {
        long idPago = idUnico();
        long notificacion = idUnico();
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, String.valueOf(idPago), 0);

        webhook(cuerpo(notificacion, "payment", String.valueOf(idPago)),
                firma(String.valueOf(idPago), "req-6", "1700000005", "otro-secreto"), "req-6")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("FIRMA_INVALIDA"));

        assertThat(webhooksGuardados(notificacion)).isZero();
        assertThat(estadoTransaccion(t.getId())).isEqualTo("PROCESANDO");
        WireMockPrueba.servidor().verify(0, getRequestedFor(urlPathEqualTo("/v1/payments/" + idPago)));
    }

    @Test
    @DisplayName("CA-5-21 una firma con otro id de pago o de petición también se rechaza")
    void firmaDeOtroMensaje() throws Exception {
        long notificacion = idUnico();

        webhook(cuerpo(notificacion, "payment", "123456"), firma("999999", "req-7", "1700000006", SECRETO), "req-7")
                .andExpect(status().isUnauthorized());
        webhook(cuerpo(notificacion, "payment", "123456"), firma("123456", "req-otro", "1700000006", SECRETO), "req-7")
                .andExpect(status().isUnauthorized());
        webhook(cuerpo(notificacion, "payment", "123456"), "basura", "req-7").andExpect(status().isUnauthorized());

        assertThat(webhooksGuardados(notificacion)).isZero();
    }

    @Test
    @DisplayName("CA-5-20 si no se puede confirmar con la pasarela responde 200 igual y el reenvío del aviso lo procesa")
    void confirmacionFallidaYReenvio() throws Exception {
        long idPago = idUnico();
        long notificacion = idUnico();
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, String.valueOf(idPago), 0);
        String cuerpo = cuerpo(notificacion, "payment", String.valueOf(idPago));
        String firma = firma(String.valueOf(idPago), "req-8", "1700000008", SECRETO);
        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago).willReturn(respuesta(503, "{}")));

        webhook(cuerpo, firma, "req-8").andExpect(status().isOk());

        assertThat(estadoTransaccion(t.getId())).isEqualTo("PROCESANDO");
        assertThat(jdbc.queryForObject("SELECT procesado_en IS NULL FROM webhook_recibido WHERE evento_id_proveedor = ?",
                Boolean.class, String.valueOf(notificacion))).isTrue();

        WireMockPrueba.servidor().stubFor(consultarPagoMp(idPago).willReturn(respuesta(200, pagoMp(idPago, "approved", "accredited"))));
        webhook(cuerpo, firma, "req-8").andExpect(status().isOk());

        assertThat(estadoTransaccion(t.getId())).isEqualTo("APROBADA");
        assertThat(webhooksGuardados(notificacion)).isEqualTo(1);
    }
}
