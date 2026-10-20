package com.fitstore.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.payment.model.CorrelativoBoleta;
import com.fitstore.payment.pruebas.PruebaRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Núcleo de 0a.2: el correlativo de la serie B001 es atómico y sin huecos (micro 3.1 e invariante 5). */
@PruebaRepositorio
class CorrelativoBoletaRepositoryTest {

    @Autowired CorrelativoBoletaRepository repositorio;
    @Autowired TransactionTemplate transaccion;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-06 la serie B001 viene sembrada en V1 con último = 0")
    void serieSembrada() {
        CorrelativoBoleta serie = repositorio.findById("B001").orElseThrow();
        assertThat(serie.getUltimo()).isZero();
    }

    @Test
    @DisplayName("CA-0a-06 siguiente() incrementa y devuelve el número en una sola sentencia")
    void siguienteIncrementa() {
        assertThat(repositorio.siguiente("B001")).isEqualTo(1);
        assertThat(repositorio.siguiente("B001")).isEqualTo(2);
        assertThat(repositorio.siguiente("B001")).isEqualTo(3);
    }

    @Test
    @DisplayName("CA-0a-06 inserta y actualiza otra serie")
    void insertaYActualizaSerie() {
        repositorio.saveAndFlush(new CorrelativoBoleta("B002", 0));

        assertThat(repositorio.siguiente("B002")).isEqualTo(1);
        assertThat(repositorio.siguiente("B001")).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("CA-0a-06 una transacción revertida no consume número: no quedan huecos")
    void revertidaNoConsume() {
        try {
            int inicial = jdbc.queryForObject("SELECT ultimo FROM correlativo_boleta WHERE serie = 'B001'", Integer.class);
            try {
                transaccion.executeWithoutResult(estado -> {
                    repositorio.siguiente("B001");
                    throw new IllegalStateException("falla al generar el PDF");
                });
            } catch (IllegalStateException esperada) {
                // la transacción se revierte
            }

            Integer siguiente = transaccion.execute(estado -> repositorio.siguiente("B001"));
            assertThat(siguiente).isEqualTo(inicial + 1);
        } finally {
            jdbc.update("UPDATE correlativo_boleta SET ultimo = 0 WHERE serie = 'B001'");
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("CA-0a-06 con 12 hilos a la vez cada número se entrega una sola vez y sin huecos")
    void concurrenciaSinHuecos() throws Exception {
        int hilos = 12;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        try {
            List<Callable<Integer>> tareas = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                tareas.add(() -> transaccion.execute(estado -> repositorio.siguiente("B001")));
            }
            List<Integer> obtenidos = new ArrayList<>();
            for (Future<Integer> f : pool.invokeAll(tareas)) {
                obtenidos.add(f.get());
            }

            assertThat(obtenidos).doesNotHaveDuplicates().containsExactlyInAnyOrder(
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            assertThat(jdbc.queryForObject("SELECT ultimo FROM correlativo_boleta WHERE serie = 'B001'", Integer.class))
                    .isEqualTo(hilos);
        } finally {
            pool.shutdownNow();
            jdbc.update("UPDATE correlativo_boleta SET ultimo = 0 WHERE serie = 'B001'");
        }
    }
}
