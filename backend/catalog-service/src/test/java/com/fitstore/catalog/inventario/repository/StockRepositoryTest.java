package com.fitstore.catalog.inventario.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.catalog.inventario.model.Stock;
import com.fitstore.catalog.pruebas.Datos;
import com.fitstore.catalog.pruebas.PruebaRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class StockRepositoryTest {

    @Autowired StockRepository repositorio;
    @Autowired ProductoRepository productos;
    @Autowired VarianteRepository variantes;
    @Autowired TallaRepository tallas;
    @Autowired ColorRepository colores;
    @Autowired JdbcTemplate jdbc;

    Producto producto;
    int contador;

    @BeforeEach
    void producto() {
        producto = productos.saveAndFlush(Datos.producto("STK-01", "Legging Stock", "legging-stock", "59.90"));
    }

    private Variante variante(String talla) {
        contador++;
        return variantes.saveAndFlush(Datos.variante(producto.getId(), tallas.findByCodigo(talla).orElseThrow().getId(),
                colores.findByNombreIgnoreCase("negro").orElseThrow().getId(), "STK-01-NEGRO-" + talla + contador));
    }

    private int fila(String columna, Long varianteId) {
        return jdbc.queryForObject("SELECT " + columna + " FROM stock WHERE variante_id = ?", Integer.class, varianteId);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository inserta el stock de una variante con mínimo NULL")
    void inserta() {
        Variante v = variante("M");
        repositorio.saveAndFlush(Datos.stock(v.getId(), 10, 0));

        Stock leido = repositorio.findById(v.getId()).orElseThrow();
        assertThat(leido.getFisico()).isEqualTo(10);
        assertThat(leido.getReservado()).isZero();
        assertThat(leido.getMinimo()).isNull();
        assertThat(leido.getActualizadoEn()).isNotNull();
        assertThat(leido.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository reserva de forma atómica cuando alcanza el disponible")
    void reservaCuandoAlcanza() {
        Variante v = variante("M");
        repositorio.saveAndFlush(Datos.stock(v.getId(), 5, 1));

        int filas = repositorio.reservarSiAlcanza(v.getId(), 4);

        assertThat(filas).isEqualTo(1);
        assertThat(fila("reservado", v.getId())).isEqualTo(5);
        assertThat(fila("fisico", v.getId())).isEqualTo(5);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository no reserva si no alcanza y deja el stock intacto")
    void noReservaSiNoAlcanza() {
        Variante v = variante("L");
        repositorio.saveAndFlush(Datos.stock(v.getId(), 5, 2));

        int filas = repositorio.reservarSiAlcanza(v.getId(), 4); // disponible = 3

        assertThat(filas).isZero();
        assertThat(fila("reservado", v.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository reserva exactamente el último disponible")
    void reservaElLimite() {
        Variante v = variante("S");
        repositorio.saveAndFlush(Datos.stock(v.getId(), 3, 2));

        assertThat(repositorio.reservarSiAlcanza(v.getId(), 1)).isEqualTo(1);
        assertThat(repositorio.reservarSiAlcanza(v.getId(), 1)).isZero();
        assertThat(fila("reservado", v.getId())).isEqualTo(3);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository devuelve 0 filas al reservar una variante sin stock registrado")
    void reservaSinFila() {
        assertThat(repositorio.reservarSiAlcanza(-1L, 1)).isZero();
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository libera lo reservado y no baja de cero")
    void liberaReservado() {
        Variante v = variante("M");
        repositorio.saveAndFlush(Datos.stock(v.getId(), 10, 4));

        assertThat(repositorio.liberar(v.getId(), 3)).isEqualTo(1);
        assertThat(fila("reservado", v.getId())).isEqualTo(1);
        assertThat(repositorio.liberar(v.getId(), 2)).isZero(); // solo hay 1 reservado
        assertThat(fila("reservado", v.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository descuenta lo vendido del físico y del reservado")
    void descuentaVenta() {
        Variante v = variante("M");
        repositorio.saveAndFlush(Datos.stock(v.getId(), 10, 4));

        assertThat(repositorio.descontar(v.getId(), 3)).isEqualTo(1);

        assertThat(fila("fisico", v.getId())).isEqualTo(7);
        assertThat(fila("reservado", v.getId())).isEqualTo(1);
        assertThat(repositorio.descontar(v.getId(), 2)).isZero(); // más que lo reservado
        assertThat(fila("fisico", v.getId())).isEqualTo(7);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository actualiza el stock mínimo y el físico con bloqueo optimista")
    void actualiza() {
        Variante v = variante("M");
        Stock s = repositorio.saveAndFlush(Datos.stock(v.getId(), 10, 0));
        Integer version = s.getVersion();
        s.setFisico(25);
        s.setMinimo(8);
        repositorio.saveAndFlush(s);

        Stock leido = repositorio.findById(v.getId()).orElseThrow();
        assertThat(leido.getFisico()).isEqualTo(25);
        assertThat(leido.getMinimo()).isEqualTo(8);
        assertThat(leido.getVersion()).isGreaterThan(version);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository lista el stock bajo con el mínimo de la variante o el de por defecto")
    void listaStockBajo() {
        Variante porDefecto = variante("XS"); // minimo NULL: usa 5 (semilla); disponible 5 -> bajo
        Variante propioAlto = variante("S"); // minimo 20, disponible 15 -> bajo
        Variante propioBajo = variante("M"); // minimo 2, disponible 3 -> normal
        Variante normal = variante("L"); // minimo NULL, disponible 6 -> normal
        repositorio.saveAndFlush(Datos.stock(porDefecto.getId(), 7, 2));
        Stock a = Datos.stock(propioAlto.getId(), 15, 0);
        a.setMinimo(20);
        repositorio.saveAndFlush(a);
        Stock b = Datos.stock(propioBajo.getId(), 3, 0);
        b.setMinimo(2);
        repositorio.saveAndFlush(b);
        repositorio.saveAndFlush(Datos.stock(normal.getId(), 6, 0));

        assertThat(repositorio.listarStockBajo()).extracting(Stock::getVarianteId)
                .contains(porDefecto.getId(), propioAlto.getId())
                .doesNotContain(propioBajo.getId(), normal.getId());
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository rechaza reservado mayor que físico (CHECK)")
    void reservadoMayorQueFisico() {
        Variante v = variante("M");

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.stock(v.getId(), 5, 6)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository rechaza un físico negativo (CHECK)")
    void fisicoNegativo() {
        Variante v = variante("M");

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.stock(v.getId(), -1, 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository rechaza un reservado negativo (CHECK)")
    void reservadoNegativo() {
        Variante v = variante("M");

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.stock(v.getId(), 5, -1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository rechaza bajar el físico por debajo de lo reservado con un UPDATE directo (CHECK)")
    void fisicoPorDebajoDeReservado() {
        Variante v = variante("M");
        repositorio.saveAndFlush(Datos.stock(v.getId(), 5, 4));

        assertThatThrownBy(() -> jdbc.update("UPDATE stock SET fisico = 3 WHERE variante_id = ?", v.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 StockRepository elimina físicamente el stock de una variante")
    void elimina() {
        Variante v = variante("M");
        repositorio.saveAndFlush(Datos.stock(v.getId(), 5, 0));

        repositorio.deleteById(v.getId());
        repositorio.flush();

        assertThat(repositorio.findById(v.getId())).isEmpty();
    }
}
