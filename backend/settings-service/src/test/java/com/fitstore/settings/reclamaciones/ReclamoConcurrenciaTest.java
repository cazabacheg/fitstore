package com.fitstore.settings.reclamaciones;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.settings.pruebas.PruebaApi;
import com.fitstore.settings.reclamaciones.dto.ReclamoRequest;
import com.fitstore.settings.reclamaciones.service.ReclamoService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** CA-3-26: 20 reclamos al mismo tiempo reciben números consecutivos, sin huecos ni repetidos. */
class ReclamoConcurrenciaTest extends PruebaApi {

    @Autowired
    ReclamoService reclamos;

    private int ultimoAntes;

    @BeforeEach
    void anotarElCorrelativo() {
        ultimoAntes = jdbc.query("SELECT ultimo FROM correlativo_reclamo WHERE anio = 2026",
                rs -> rs.next() ? rs.getInt(1) : 0);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM reclamo WHERE anio = 2026 AND correlativo > ?", ultimoAntes);
        if (ultimoAntes == 0) {
            jdbc.update("DELETE FROM correlativo_reclamo WHERE anio = 2026");
        } else {
            jdbc.update("UPDATE correlativo_reclamo SET ultimo = ? WHERE anio = 2026", ultimoAntes);
        }
        jdbc.update("DELETE FROM outbox_evento WHERE tipo = 'ReclamoRegistrado'");
    }

    private static ReclamoRequest solicitud(int i) {
        return new ReclamoRequest(com.fitstore.settings.reclamaciones.model.TipoReclamo.RECLAMO,
                new com.fitstore.settings.reclamaciones.dto.ConsumidorRequest("Rosa" + i, "Díaz",
                        com.fitstore.settings.reclamaciones.model.TipoDocumento.DNI, "87654321", "Calle Mercaderes 456",
                        "912345678", "rosa" + i + "@correo.com", false, null),
                new com.fitstore.settings.reclamaciones.dto.BienContratadoRequest(
                        com.fitstore.settings.reclamaciones.model.TipoBien.PRODUCTO, "Casaca Wind", BigDecimal.TEN, null),
                "La casaca llegó con el cierre roto.", "Cambio por una nueva.");
    }

    @Test
    @DisplayName("CA-3-26 20 reclamos enviados a la vez reciben números consecutivos, sin huecos ni repetidos")
    void veinteSimultaneos() throws Exception {
        int cantidad = 20;
        ExecutorService hilos = Executors.newFixedThreadPool(cantidad);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<String>> resultados = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            int n = i;
            resultados.add(hilos.submit(() -> {
                salida.await();
                return reclamos.registrar(solicitud(n), "203.0.113." + n).numero();
            }));
        }
        salida.countDown();
        List<String> numeros = new ArrayList<>();
        for (Future<String> f : resultados) {
            numeros.add(f.get(60, TimeUnit.SECONDS));
        }
        hilos.shutdown();

        assertThat(numeros).doesNotHaveDuplicates().hasSize(cantidad);
        List<Integer> correlativos = numeros.stream().map(n -> Integer.parseInt(n.substring(n.lastIndexOf('-') + 1)))
                .sorted().toList();
        assertThat(correlativos).containsExactlyElementsOf(
                java.util.stream.IntStream.rangeClosed(ultimoAntes + 1, ultimoAntes + cantidad).boxed().toList());
        assertThat(numeros).allMatch(n -> n.matches("RC-2026-\\d{6}"));
        assertThat(jdbc.queryForObject("SELECT ultimo FROM correlativo_reclamo WHERE anio = 2026", Integer.class))
                .isEqualTo(ultimoAntes + cantidad);
    }

    @Test
    @DisplayName("CA-3-26 si el registro falla, el número no se consume (sin huecos)")
    void sinHuecosSiFalla() {
        ReclamoRequest invalida = new ReclamoRequest(com.fitstore.settings.reclamaciones.model.TipoReclamo.RECLAMO,
                new com.fitstore.settings.reclamaciones.dto.ConsumidorRequest("Rosa", "Díaz",
                        com.fitstore.settings.reclamaciones.model.TipoDocumento.DNI, "87654321", "Calle Mercaderes 456",
                        "912345678", "rosa@correo.com", false, null),
                // Un monto que no cabe en numeric(12,2): la base lo rechaza después de entregar el número
                new com.fitstore.settings.reclamaciones.dto.BienContratadoRequest(
                        com.fitstore.settings.reclamaciones.model.TipoBien.PRODUCTO, "Casaca Wind",
                        new BigDecimal("99999999999999.99"), null),
                "La casaca llegó con el cierre roto.", "Cambio por una nueva.");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> reclamos.registrar(invalida, "203.0.113.9"))
                .isInstanceOf(RuntimeException.class);
        String numero = reclamos.registrar(solicitud(1), "203.0.113.1").numero();

        assertThat(numero).isEqualTo("RC-2026-%06d".formatted(ultimoAntes + 1));
    }
}
