package com.fitstore.catalog.inventario.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.catalog.pruebas.Datos;
import com.fitstore.catalog.pruebas.PruebaRepositorio;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Prueba de concurrencia de la reserva atómica (micro 4.6 y 9). Fuera de la transacción de prueba: cada hilo
 * confirma la suya y las filas se limpian a mano al terminar.
 */
@PruebaRepositorio
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class StockConcurrenciaTest {

    private static final int HILOS = 20;

    @Autowired StockRepository stock;
    @Autowired ProductoRepository productos;
    @Autowired VarianteRepository variantes;
    @Autowired TallaRepository tallas;
    @Autowired ColorRepository colores;
    @Autowired TransactionTemplate transaccion;
    @Autowired JdbcTemplate jdbc;

    Producto producto;
    final List<Long> varianteIds = new ArrayList<>();

    @AfterEach
    void limpiar() {
        for (Long id : varianteIds) {
            jdbc.update("DELETE FROM stock WHERE variante_id = ?", id);
            jdbc.update("DELETE FROM variante WHERE id = ?", id);
        }
        if (producto != null) {
            jdbc.update("DELETE FROM producto WHERE id = ?", producto.getId());
        }
    }

    private Variante variante(String talla, int fisico) {
        if (producto == null) {
            producto = productos.saveAndFlush(Datos.producto("CON-01", "Legging Concurrencia", "legging-concurrencia", "59.90"));
        }
        Variante v = variantes.saveAndFlush(Datos.variante(producto.getId(), tallas.findByCodigo(talla).orElseThrow().getId(),
                colores.findByNombreIgnoreCase("negro").orElseThrow().getId(), "CON-01-NEGRO-" + talla));
        varianteIds.add(v.getId());
        stock.saveAndFlush(Datos.stock(v.getId(), fisico, 0));
        return v;
    }

    private int columna(String nombre, Long varianteId) {
        return jdbc.queryForObject("SELECT " + nombre + " FROM stock WHERE variante_id = ?", Integer.class, varianteId);
    }

    /** Lanza las tareas a la vez (compuerta común) y devuelve cuántas devolvieron {@code true}. */
    private long ejecutarEnParalelo(Callable<Boolean> tarea) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch listos = new CountDownLatch(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Boolean>> resultados = new ArrayList<>();
            for (int i = 0; i < HILOS; i++) {
                resultados.add(pool.submit(() -> {
                    listos.countDown();
                    salida.await();
                    return tarea.call();
                }));
            }
            listos.await();
            salida.countDown();
            long exitos = 0;
            for (Future<Boolean> f : resultados) {
                if (f.get()) {
                    exitos++;
                }
            }
            return exitos;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("CA-0a-06 20 hilos reservan 1 unidad de una variante con 5 disponibles: exactamente 5 éxitos, 15 rechazos y reservado = 5")
    void veinteHilosCincoDisponibles() throws Exception {
        Variante v = variante("M", 5);

        long exitos = ejecutarEnParalelo(() -> stock.reservarSiAlcanza(v.getId(), 1) == 1);

        assertThat(exitos).isEqualTo(5);
        assertThat(columna("reservado", v.getId())).isEqualTo(5);
        assertThat(columna("fisico", v.getId())).isEqualTo(5);
    }

    @Test
    @DisplayName("CA-0a-06 con reservas de varias líneas todo o nada (líneas ordenadas por variante) no hay sobreventa ni interbloqueo")
    void variasLineasTodoONada() throws Exception {
        Variante a = variante("S", 5);
        Variante b = variante("L", 5);

        long exitos = ejecutarEnParalelo(() -> transaccion.execute(estado -> {
            boolean ok = stock.reservarSiAlcanza(a.getId(), 1) == 1 && stock.reservarSiAlcanza(b.getId(), 1) == 1;
            if (!ok) {
                estado.setRollbackOnly();
            }
            return ok;
        }));

        assertThat(exitos).isEqualTo(5);
        assertThat(columna("reservado", a.getId())).isEqualTo(5);
        assertThat(columna("reservado", b.getId())).isEqualTo(5);
    }
}
