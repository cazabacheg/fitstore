package com.fitstore.payment.provider;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.payment.config.PropiedadesPagos;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.provider.mercadopago.MercadoPagoProvider;
import com.fitstore.payment.pruebas.WireMockPrueba;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Adaptador de Mercado Pago contra su API simulada con WireMock (HU-5-05, CA-5-15). */
class MercadoPagoProviderTest {

    private final WireMockServer mp = WireMockPrueba.servidor();

    private MercadoPagoProvider proveedor(Duration timeout) {
        return new MercadoPagoProvider(new PropiedadesPagos.MercadoPago("TEST-pub", "TEST-token", "secreto",
                WireMockPrueba.url(), timeout));
    }

    @BeforeEach
    void limpiar() {
        mp.resetAll();
    }

    private static SolicitudCargo cargo(String monto) {
        return new SolicitudCargo(UUID.randomUUID(), "b6a1f0de-0000-4000-8000-000000000001", "FS-000124",
                new BigDecimal(monto), "tok_brick_123", Medio.TARJETA, "ana@correo.pe", "clave-idem-1",
                Map.of("payment_method_id", "visa", "installments", 1, "issuer_id", "310", "cvv", "123"));
    }

    @Test
    @DisplayName("CA-5-15 cobra con el token del Brick, el monto en decimales y la misma Idempotency-Key como X-Idempotency-Key")
    void cobraConTokenYClave() {
        mp.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"id":1234567,"status":"approved","status_detail":"accredited","payment_method_id":"visa",
                         "payment_type_id":"credit_card","card":{"last_four_digits":"1111"}}""")));

        ResultadoCargo r = proveedor(Duration.ofSeconds(5)).crearCargo(cargo("59.90"));

        assertThat(r.estado()).isEqualTo(EstadoTransaccion.APROBADA);
        assertThat(r.idProveedor()).isEqualTo("1234567");
        assertThat(r.descripcionMedio()).isEqualTo("Visa ****1111");
        mp.verify(postRequestedFor(urlEqualTo("/v1/payments"))
                .withHeader("Authorization", equalTo("Bearer TEST-token"))
                .withHeader("X-Idempotency-Key", equalTo("clave-idem-1"))
                .withRequestBody(com.github.tomakehurst.wiremock.client.WireMock.notMatching("(?s).*cvv.*"))
                .withRequestBody(equalToJson("""
                        {"transaction_amount":59.90,"token":"tok_brick_123","installments":1,"payment_method_id":"visa",
                         "issuer_id":"310","payer":{"email":"ana@correo.pe"},
                         "external_reference":"b6a1f0de-0000-4000-8000-000000000001"}""", true, true)));
    }

    @Test
    @DisplayName("CA-5-15 un cargo rechazado por fondos insuficientes devuelve RECHAZADA con motivo legible")
    void rechazoLegible() {
        mp.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":99,\"status\":\"rejected\",\"status_detail\":\"cc_rejected_insufficient_amount\"}")));

        ResultadoCargo r = proveedor(Duration.ofSeconds(5)).crearCargo(cargo("119.80"));

        assertThat(r.estado()).isEqualTo(EstadoTransaccion.RECHAZADA);
        assertThat(r.mensajeLegible()).isEqualTo("Fondos insuficientes");
        assertThat(r.codigo()).isEqualTo("cc_rejected_insufficient_amount");
    }

    @Test
    @DisplayName("CA-5-15 un pago en proceso queda PROCESANDO")
    void enProceso() {
        mp.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":77,\"status\":\"in_process\",\"status_detail\":\"pending_contingency\"}")));

        assertThat(proveedor(Duration.ofSeconds(5)).crearCargo(cargo("119.80")).estado())
                .isEqualTo(EstadoTransaccion.PROCESANDO);
    }

    @Test
    @DisplayName("CA-5-15 consulta el estado de un pago por su id")
    void consultaElEstado() {
        mp.stubFor(get(urlPathEqualTo("/v1/payments/1234567")).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":1234567,\"status\":\"approved\",\"status_detail\":\"accredited\"}")));

        assertThat(proveedor(Duration.ofSeconds(5)).consultarEstado("1234567").estado())
                .isEqualTo(EstadoTransaccion.APROBADA);
        mp.verify(getRequestedFor(urlPathEqualTo("/v1/payments/1234567"))
                .withHeader("Authorization", equalTo("Bearer TEST-token")));
    }

    @Test
    @DisplayName("CA-5-15 sin respuesta a tiempo lanza PasarelaSinRespuestaException y no reintenta")
    void sinRespuesta() {
        mp.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(201).withFixedDelay(1500)
                .withBody("{}")));

        assertThatThrownBy(() -> proveedor(Duration.ofMillis(500)).crearCargo(cargo("119.80")))
                .isInstanceOf(PasarelaSinRespuestaException.class);
        // WireMock anota el pedido cuando termina de responder (tras la demora): se espera a que aparezca
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(12))
                .untilAsserted(() -> mp.verify(1, postRequestedFor(urlEqualTo("/v1/payments"))));
    }

    @Test
    @DisplayName("CA-5-15 una respuesta 5xx de la pasarela cuenta como sin respuesta")
    void errorDelServidor() {
        mp.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> proveedor(Duration.ofSeconds(5)).crearCargo(cargo("119.80")))
                .isInstanceOf(PasarelaSinRespuestaException.class);
    }

    @Test
    @DisplayName("CA-5-15 identifica a Mercado Pago, ofrece tarjeta y expone su clave pública")
    void identidad() {
        MercadoPagoProvider p = proveedor(Duration.ofSeconds(5));

        assertThat(p.proveedor()).isEqualTo(Proveedor.MERCADO_PAGO);
        assertThat(p.clavePublica()).isEqualTo("TEST-pub");
        assertThat(p.tieneCredenciales()).isTrue();
        assertThat(p.medios()).contains(Medio.TARJETA);
    }

    private static com.github.tomakehurst.wiremock.client.MappingBuilder consulta(String ruta) {
        return get(urlPathEqualTo(ruta));
    }

    private static String firma(String secreto, String idPago, String idPeticion, String ts) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secreto.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String manifiesto = "id:" + idPago + ";request-id:" + idPeticion + ";ts:" + ts + ";";
        return "ts=" + ts + ",v1=" + java.util.HexFormat.of().formatHex(
                mac.doFinal(manifiesto.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("CA-5-15 una solicitud que la pasarela rechaza con 4xx (datos inválidos) queda RECHAZADA sin id")
    void solicitudInvalida() {
        mp.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(400).withBody("{}")));

        ResultadoCargo r = proveedor(Duration.ofSeconds(5)).crearCargo(cargo("119.80"));

        assertThat(r.estado()).isEqualTo(EstadoTransaccion.RECHAZADA);
        assertThat(r.codigo()).isEqualTo("http_400");
        assertThat(r.idProveedor()).isNull();
    }

    @Test
    @DisplayName("CA-5-15 los códigos de rechazo se traducen a mensajes legibles")
    void mensajesLegibles() {
        java.util.Map<String, String> esperados = java.util.Map.of(
                "cc_rejected_bad_filled_date", "Tarjeta vencida o fecha incorrecta",
                "cc_rejected_bad_filled_card_number", "Revisa los datos de tu tarjeta",
                "cc_rejected_call_for_authorize", "Debes autorizar el pago con tu banco",
                "cc_rejected_card_disabled", "Tu tarjeta está inactiva; actívala con tu banco",
                "cc_rejected_max_attempts", "Superaste el máximo de intentos; prueba con otra tarjeta",
                "cc_rejected_high_risk", "Pago rechazado por seguridad; prueba con otro medio",
                "cc_rejected_duplicated_payment", "Ya hiciste un pago igual hace poco",
                "cc_rejected_otro_codigo", "Tarjeta rechazada por el banco");
        esperados.forEach((detalle, mensaje) -> {
            mp.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"id\":5,\"status\":\"rejected\",\"status_detail\":\"" + detalle + "\"}")));
            assertThat(proveedor(Duration.ofSeconds(5)).crearCargo(cargo("10.00")).mensajeLegible())
                    .as(detalle).isEqualTo(mensaje);
        });
    }

    @Test
    @DisplayName("CA-5-15 un pago con saldo de Mercado Pago se describe como tal")
    void saldoDeMercadoPago() {
        mp.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":6,\"status\":\"approved\",\"status_detail\":\"accredited\",\"payment_method_id\":\"account_money\"}")));

        assertThat(proveedor(Duration.ofSeconds(5)).crearCargo(cargo("10.00")).descripcionMedio())
                .isEqualTo("Saldo Mercado Pago");
    }

    @Test
    @DisplayName("CA-5-22 consultar un pago que Mercado Pago no reconoce lanza ConsultaRechazadaException; con 5xx, sin respuesta")
    void consultaFallida() {
        mp.stubFor(consulta("/v1/payments/404").willReturn(aResponse().withStatus(404).withBody("{}")));
        mp.stubFor(consulta("/v1/payments/503").willReturn(aResponse().withStatus(503)));
        MercadoPagoProvider p = proveedor(Duration.ofSeconds(5));

        assertThatThrownBy(() -> p.consultarEstado("404")).isInstanceOf(ConsultaRechazadaException.class);
        assertThatThrownBy(() -> p.consultarEstado("503")).isInstanceOf(PasarelaSinRespuestaException.class);
    }

    @Test
    @DisplayName("CA-5-22 buscar un cargo por su referencia: lo encuentra, no hay rastro, o la pasarela falla")
    void busquedaPorReferencia() {
        MercadoPagoProvider p = proveedor(Duration.ofSeconds(5));
        mp.stubFor(consulta("/v1/payments/search").withQueryParam("external_reference", equalTo("hay"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[{\"id\":9,\"status\":\"approved\",\"status_detail\":\"accredited\"}]}")));
        mp.stubFor(consulta("/v1/payments/search").withQueryParam("external_reference", equalTo("vacio"))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                        .withBody("{\"results\":[]}")));
        mp.stubFor(consulta("/v1/payments/search").withQueryParam("external_reference", equalTo("rechazo"))
                .willReturn(aResponse().withStatus(401).withBody("{}")));
        mp.stubFor(consulta("/v1/payments/search").withQueryParam("external_reference", equalTo("caido"))
                .willReturn(aResponse().withStatus(500)));

        assertThat(p.consultarPorReferencia("hay").estado()).isEqualTo(EstadoTransaccion.APROBADA);
        assertThat(p.consultarPorReferencia("vacio")).isNull();
        assertThatThrownBy(() -> p.consultarPorReferencia("rechazo")).isInstanceOf(ConsultaRechazadaException.class);
        assertThatThrownBy(() -> p.consultarPorReferencia("caido")).isInstanceOf(PasarelaSinRespuestaException.class);
    }

    @Test
    @DisplayName("CA-5-21 la firma se valida con el secreto, el id del pago, el id de la petición y el ts")
    void validaLaFirma() throws Exception {
        MercadoPagoProvider p = proveedor(Duration.ofSeconds(5));
        java.util.Map<String, Object> cuerpo = java.util.Map.of("id", 77, "type", "payment", "data", java.util.Map.of("id", "ABC123"));

        EventoProveedor evento = p.verificarWebhook(new WebhookEntrante(firma("secreto", "abc123", "req", "10"), "req", cuerpo));

        assertThat(evento.eventoId()).isEqualTo("77");
        assertThat(evento.idPago()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("CA-5-21 sin id de aviso usa el ts y el pago; un aviso que no es de pago no trae id de pago")
    void avisoSinIdYOtrosTipos() throws Exception {
        MercadoPagoProvider p = proveedor(Duration.ofSeconds(5));
        java.util.Map<String, Object> sinId = java.util.Map.of("topic", "payment", "data", java.util.Map.of("id", "55"));
        java.util.Map<String, Object> otro = java.util.Map.of("type", "plan", "data", java.util.Map.of("id", "55"));

        EventoProveedor a = p.verificarWebhook(new WebhookEntrante(firma("secreto", "55", "r", "9"), "r", sinId));
        EventoProveedor b = p.verificarWebhook(new WebhookEntrante(firma("secreto", "55", "r", "9"), "r", otro));

        assertThat(a.eventoId()).isEqualTo("9-55");
        assertThat(a.idPago()).isEqualTo("55");
        assertThat(b.idPago()).isNull();
    }

    @Test
    @DisplayName("CA-5-21 se rechaza una firma ausente, mal formada, sin id de pago o con un secreto vacío")
    void firmasInvalidas() throws Exception {
        MercadoPagoProvider p = proveedor(Duration.ofSeconds(5));
        java.util.Map<String, Object> cuerpo = java.util.Map.of("id", 1, "type", "payment", "data", java.util.Map.of("id", "1"));
        MercadoPagoProvider sinSecreto = new MercadoPagoProvider(new PropiedadesPagos.MercadoPago("pub", "token", "",
                WireMockPrueba.url(), Duration.ofSeconds(5)));

        assertThatThrownBy(() -> p.verificarWebhook(new WebhookEntrante(null, "r", cuerpo))).isInstanceOf(FirmaInvalidaException.class);
        assertThatThrownBy(() -> p.verificarWebhook(new WebhookEntrante("ts=1", "r", cuerpo))).isInstanceOf(FirmaInvalidaException.class);
        assertThatThrownBy(() -> p.verificarWebhook(new WebhookEntrante(firma("secreto", "1", "r", "1"), "r", java.util.Map.of())))
                .isInstanceOf(FirmaInvalidaException.class);
        assertThatThrownBy(() -> sinSecreto.verificarWebhook(new WebhookEntrante(firma("secreto", "1", "r", "1"), "r", cuerpo)))
                .isInstanceOf(FirmaInvalidaException.class);
    }

    @Test
    @DisplayName("CA-5-15 sin credenciales el proveedor no se ofrece")
    void sinCredenciales() {
        MercadoPagoProvider sin = new MercadoPagoProvider(new PropiedadesPagos.MercadoPago("", "", "", WireMockPrueba.url(),
                Duration.ofSeconds(5)));

        assertThat(sin.tieneCredenciales()).isFalse();
    }
}
