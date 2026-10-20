package com.fitstore.settings.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.settings.pruebas.Datos;
import com.fitstore.settings.pruebas.PruebaDatos;
import com.fitstore.settings.reclamaciones.model.Reclamo;
import com.fitstore.settings.reclamaciones.repository.CorrelativoReclamoRepository;
import com.fitstore.settings.reclamaciones.repository.ReclamoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Núcleo de settings: el correlativo de reclamos es atómico, sin huecos ni repetidos (invariante 7).
 * Estas pruebas confirman transacciones reales (varios hilos), así que limpian lo que escriben.
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CorrelativoReclamoRepositoryTest extends PruebaDatos {

    private static final int HILOS = 20;

    @Autowired CorrelativoReclamoRepository correlativos;
    @Autowired ReclamoRepository reclamos;
    @Autowired PlatformTransactionManager gestor;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM reclamo");
        jdbc.update("DELETE FROM correlativo_reclamo");
    }

    private TransactionTemplate transaccionNueva() {
        TransactionTemplate t = new TransactionTemplate(gestor);
        t.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return t;
    }

    @Test
    @DisplayName("CA-0a-06 el primer reclamo del año recibe 1 y los siguientes 2, 3…")
    void secuencialPorAnio() {
        TransactionTemplate tx = transaccionNueva();

        assertThat(siguienteEnTransaccion(tx, 2026)).isEqualTo(1);
        assertThat(siguienteEnTransaccion(tx, 2026)).isEqualTo(2);
        assertThat(siguienteEnTransaccion(tx, 2026)).isEqualTo(3);
    }

    @Test
    @DisplayName("CA-0a-06 cada año lleva su propio contador")
    void aniosIndependientes() {
        TransactionTemplate tx = transaccionNueva();

        tx.execute(s -> correlativos.siguiente(2026));
        tx.execute(s -> correlativos.siguiente(2026));

        assertThat(siguienteEnTransaccion(tx, 2027)).isEqualTo(1);
        assertThat(siguienteEnTransaccion(tx, 2026)).isEqualTo(3);
    }

    @Test
    @DisplayName("CA-0a-06 si la transacción del reclamo se revierte, el número no se pierde (sin huecos)")
    void sinHuecosSiSeRevierte() {
        TransactionTemplate tx = transaccionNueva();
        assertThat(siguienteEnTransaccion(tx, 2026)).isEqualTo(1);

        assertThatThrownBy(() -> tx.execute(s -> {
            correlativos.siguiente(2026); // tomaría el 2
            throw new IllegalStateException("falla el registro del reclamo");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(siguienteEnTransaccion(tx, 2026)).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-0a-06 20 reclamos simultáneos reciben los correlativos 1 a 20 sin huecos ni repetidos")
    void concurrenciaSoloCorrelativo() throws Exception {
        List<Integer> obtenidos = ejecutarEnParalelo(n -> transaccionNueva().execute(s -> correlativos.siguiente(2026)));

        assertThat(new TreeSet<>(obtenidos)).hasSize(HILOS);
        assertThat(obtenidos).containsExactlyInAnyOrderElementsOf(rango(1, HILOS));
        assertThat(jdbc.queryForObject("SELECT ultimo FROM correlativo_reclamo WHERE anio = 2026", Integer.class))
                .isEqualTo(HILOS);
    }

    @Test
    @DisplayName("CA-0a-06 20 reclamos completos en paralelo quedan numerados RC-2026-000001 a 000020")
    void concurrenciaConReclamosCompletos() throws Exception {
        ejecutarEnParalelo(n -> transaccionNueva().execute(s -> {
            int correlativo = correlativos.siguiente(2026);
            Reclamo r = Datos.reclamo(2026, correlativo);
            reclamos.saveAndFlush(r);
            return correlativo;
        }));

        List<String> numeros = jdbc.queryForList("SELECT numero FROM reclamo ORDER BY correlativo", String.class);
        assertThat(numeros).hasSize(HILOS);
        assertThat(numeros.getFirst()).isEqualTo("RC-2026-000001");
        assertThat(numeros.getLast()).isEqualTo("RC-2026-000020");
        assertThat(new TreeSet<>(numeros)).hasSize(HILOS);
    }

    private int siguienteEnTransaccion(TransactionTemplate tx, int anio) {
        Integer n = tx.execute(s -> correlativos.siguiente(anio));
        return n;
    }

    private List<Integer> rango(int desde, int hasta) {
        List<Integer> r = new ArrayList<>();
        for (int i = desde; i <= hasta; i++) {
            r.add(i);
        }
        return r;
    }

    private List<Integer> ejecutarEnParalelo(java.util.function.IntFunction<Integer> tarea) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (int i = 0; i < HILOS; i++) {
                final int n = i;
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return tarea.apply(n);
                }));
            }
            salida.countDown();
            List<Integer> resultados = new ArrayList<>();
            for (Future<Integer> f : futuros) {
                resultados.add(f.get());
            }
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }
}
