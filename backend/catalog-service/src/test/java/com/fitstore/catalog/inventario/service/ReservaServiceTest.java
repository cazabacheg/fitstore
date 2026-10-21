package com.fitstore.catalog.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.catalog.inventario.dto.ItemReservaDto;
import com.fitstore.catalog.inventario.dto.ReservaRequest;
import com.fitstore.catalog.pruebas.DatosConfirmados;
import com.fitstore.catalog.pruebas.PostgresPrueba;
import com.fitstore.common.api.ConflictoNegocioException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Reserva atómica con el servicio real y la base de datos real, sin transacción de prueba: cada hilo confirma la suya
 * (HU-2-10: CA-2-47, y el todo o nada de CA-2-46 bajo concurrencia).
 */
@SpringBootTest
@ActiveProfiles("sin-config-server")
@ContextConfiguration(initializers = PostgresPrueba.class)
class ReservaServiceTest {

    private static final int HILOS = 20;

    @Autowired ReservaService reservas;
    @Autowired JdbcTemplate jdbc;

    DatosConfirmados datos;

    @BeforeEach
    void preparar() {
        datos = new DatosConfirmados(jdbc);
    }

    @AfterEach
    void limpiar() {
        datos.limpiar();
    }

    private static ReservaRequest pedido(Long... varianteIds) {
        List<ItemReservaDto> items = new ArrayList<>();
        for (Long id : varianteIds) {
            items.add(new ItemReservaDto(id, 1));
        }
        return new ReservaRequest(UUID.randomUUID(), Instant.now().plusSeconds(1800), items);
    }

    /** Lanza las tareas a la vez y devuelve cuántas terminaron sin error y cuántas con el 409 de falta de stock. */
    private int[] ejecutarEnParalelo(Callable<Object> tarea) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Object>> resultados = new ArrayList<>();
            for (int i = 0; i < HILOS; i++) {
                resultados.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await();
                    return tarea.call();
                }));
            }
            listos.await();
            salida.countDown();
            int exitos = 0;
            int rechazos = 0;
            for (Future<Object> f : resultados) {
                try {
                    f.get();
                    exitos++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(ConflictoNegocioException.class);
                    assertThat(((ConflictoNegocioException) e.getCause()).codigo()).isEqualTo("SIN_STOCK");
                    rechazos++;
                }
            }
            return new int[] {exitos, rechazos};
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("CA-2-47 20 reservas simultáneas de 1 unidad sobre una variante con 5 disponibles: exactamente 5 tienen éxito y reservado queda en 5")
    void veinteReservasCincoDisponibles() throws Exception {
        long variante = datos.variante("CON01", 5, 0);

        int[] resultado = ejecutarEnParalelo(() -> reservas.reservar(pedido(variante)));

        assertThat(resultado[0]).isEqualTo(5);
        assertThat(resultado[1]).isEqualTo(15);
        assertThat(datos.reservado(variante)).isEqualTo(5);
        assertThat(datos.fisico(variante)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva WHERE estado = 'ACTIVA'", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva_item", Integer.class)).isEqualTo(5);
    }

    @Test
    @DisplayName("CA-2-47 con pedidos de varias líneas es todo o nada: lo que se rechaza no deja unidades reservadas de más")
    void variasLineasBajoConcurrencia() throws Exception {
        long abundante = datos.variante("CON02", 10, 0);
        long escasa = datos.variante("CON03", 3, 0);

        int[] resultado = ejecutarEnParalelo(() -> reservas.reservar(pedido(abundante, escasa)));

        assertThat(resultado[0]).isEqualTo(3);
        assertThat(resultado[1]).isEqualTo(17);
        assertThat(datos.reservado(escasa)).isEqualTo(3);
        assertThat(datos.reservado(abundante)).isEqualTo(3);
    }

    @Test
    @DisplayName("CA-2-48 20 reintentos simultáneos del mismo pedidoId reservan una sola vez")
    void reintentosConcurrentesDelMismoPedido() throws Exception {
        long variante = datos.variante("CON04", 10, 0);
        ReservaRequest unico = pedido(variante);

        int[] resultado = ejecutarEnParalelo(() -> reservas.reservar(unico));

        assertThat(resultado[0]).isEqualTo(HILOS);
        assertThat(datos.reservado(variante)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reserva", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-2-46 sin stock lanza el 409 SIN_STOCK con los faltantes y no deja nada reservado")
    void sinStockLanzaConflicto() {
        long variante = datos.variante("CON05", 1, 0);

        assertThatThrownBy(() -> reservas.reservar(new ReservaRequest(UUID.randomUUID(), Instant.now().plusSeconds(60),
                List.of(new ItemReservaDto(variante, 5)))))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("Sin stock");
        assertThat(datos.reservado(variante)).isZero();
    }
}
