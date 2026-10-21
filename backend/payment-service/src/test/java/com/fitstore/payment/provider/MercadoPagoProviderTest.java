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
        mp.stubFor(post(urlEqualTo("/v1/payments")).willReturn(aResponse().withStatus(201).withFixedDelay(2500)
                .withBody("{}")));

        assertThatThrownBy(() -> proveedor(Duration.ofMillis(500)).crearCargo(cargo("119.80")))
                .isInstanceOf(PasarelaSinRespuestaException.class);
        // WireMock anota el pedido cuando termina de responder (tras la demora): se espera a que aparezca
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(6))
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
}
