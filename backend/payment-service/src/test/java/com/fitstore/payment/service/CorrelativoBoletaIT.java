package com.fitstore.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.BoletaApiBase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 20 pagos confirmados a la vez reciben correlativos consecutivos, sin huecos ni repetidos (CA-5-27). */
class CorrelativoBoletaIT extends BoletaApiBase {

    @Autowired BoletaService boletas;

    @Test
    @DisplayName("CA-5-27 veinte boletas emitidas a la vez reciben correlativos consecutivos sin huecos ni repetidos")
    void veinteConcurrentes() throws Exception {
        UUID clienta = UUID.randomUUID();
        List<IntencionPago> pagadas = new ArrayList<>();
        for (int n = 0; n < 20; n++) {
            pagadas.add(intencionPagada(clienta));
        }

        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(20);
        List<Future<Integer>> emisiones = new ArrayList<>();
        for (IntencionPago pagada : pagadas) {
            emisiones.add(hilos.submit(() -> {
                salida.await();
                return boletas.emitir(pagada.getId()).getCorrelativo();
            }));
        }
        salida.countDown();
        List<Integer> correlativos = new ArrayList<>();
        for (Future<Integer> emision : emisiones) {
            correlativos.add(emision.get());
        }
        hilos.shutdown();

        assertThat(correlativos).doesNotHaveDuplicates().hasSize(20);
        int minimo = correlativos.stream().mapToInt(Integer::intValue).min().orElseThrow();
        int maximo = correlativos.stream().mapToInt(Integer::intValue).max().orElseThrow();
        assertThat(maximo - minimo).isEqualTo(19);
        assertThat(jdbc.queryForObject("SELECT ultimo FROM correlativo_boleta WHERE serie = 'B001'", Integer.class))
                .isGreaterThanOrEqualTo(maximo);
    }
}
