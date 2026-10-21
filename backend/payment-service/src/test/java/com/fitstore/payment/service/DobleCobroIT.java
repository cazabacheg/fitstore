package com.fitstore.payment.service;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.PagoApiBase;
import com.fitstore.payment.pruebas.Sesiones;
import com.fitstore.payment.pruebas.WireMockPrueba;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Dos cobros simultáneos del mismo pedido (dos pestañas): uno se aprueba y el otro recibe 409 (CA-5-16). */
class DobleCobroIT extends PagoApiBase {

    private final UUID clienta = UUID.randomUUID();

    @Test
    @DisplayName("CA-5-16 dos cobros simultáneos con claves distintas: uno 200, el otro 409 YA_PAGADO, una sola transacción APROBADA")
    void dosHilos() throws Exception {
        // la pasarela tarda 400 ms: el segundo cobro llega mientras el primero tiene bloqueada la intención
        WireMockPrueba.servidor().stubFor(crearPagoMp().willReturn(
                respuesta(201, pagoMp(801, "approved", "accredited")).withFixedDelay(400)));
        IntencionPago i = intencion(clienta);
        String cuerpo = "{\"proveedor\":\"MERCADO_PAGO\",\"medio\":\"TARJETA\",\"token\":\"tok_brick_abc123\"}";

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int n = 0; n < 2; n++) {
            Callable<Integer> cobro = () -> {
                salida.await();
                return mvc.perform(post("/api/v1/pagos/" + i.getCodigoPedido() + "/cargos")
                                .with(Sesiones.cliente(clienta)).header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                        .andReturn().getResponse().getStatus();
            };
            resultados.add(hilos.submit(cobro));
        }
        salida.countDown();
        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> f : resultados) {
            estados.add(f.get());
        }
        hilos.shutdown();

        assertThat(estados).containsExactlyInAnyOrder(200, 409);
        assertThat(contarTransacciones(i.getId(), "APROBADA")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM transaccion WHERE intencion_id = ?", Integer.class, i.getId()))
                .isEqualTo(1);
        WireMockPrueba.servidor().verify(1, postRequestedFor(urlEqualTo("/v1/payments")));
    }
}
