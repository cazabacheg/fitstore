package com.fitstore.catalog.catalogo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.pruebas.Datos;
import com.fitstore.catalog.pruebas.PruebaRepositorio;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@PruebaRepositorio
class VarianteRepositoryTest {

    @Autowired VarianteRepository repositorio;
    @Autowired ProductoRepository productos;
    @Autowired TallaRepository tallas;
    @Autowired ColorRepository colores;

    Producto producto;
    Talla m;
    Talla l;
    Color negro;
    Color azul;

    @BeforeEach
    void datos() {
        producto = productos.saveAndFlush(Datos.producto("LEG-10", "Legging Pro", "legging-pro-v", "59.90"));
        m = tallas.findByCodigo("M").orElseThrow();
        l = tallas.findByCodigo("L").orElseThrow();
        negro = colores.findByNombreIgnoreCase("negro").orElseThrow();
        azul = colores.findByNombreIgnoreCase("azul").orElseThrow();
    }

    @Test
    @DisplayName("CA-0a-06 VarianteRepository inserta una variante que usa el precio base (precio NULL)")
    void inserta() {
        Variante v = repositorio.saveAndFlush(Datos.variante(producto.getId(), m.getId(), negro.getId(), "LEG-10-NEGRO-M"));

        Variante leida = repositorio.findById(v.getId()).orElseThrow();
        assertThat(leida.getSku()).isEqualTo("LEG-10-NEGRO-M");
        assertThat(leida.getPrecio()).isNull();
        assertThat(leida.isActivo()).isTrue();
        assertThat(leida.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 VarianteRepository inserta la matriz de tallas por colores")
    void insertaMatriz() {
        List<Variante> matriz = new ArrayList<>();
        for (Talla t : List.of(m, l)) {
            for (Color c : List.of(negro, azul)) {
                matriz.add(Datos.variante(producto.getId(), t.getId(), c.getId(),
                        "LEG-10-" + c.getNombre().toUpperCase() + "-" + t.getCodigo()));
            }
        }
        repositorio.saveAllAndFlush(matriz);

        assertThat(repositorio.findByProductoIdOrderByIdAsc(producto.getId())).hasSize(4);
    }

    @Test
    @DisplayName("CA-0a-06 VarianteRepository actualiza el precio de una variante")
    void actualizaPrecio() {
        Variante v = repositorio.saveAndFlush(Datos.variante(producto.getId(), l.getId(), negro.getId(), "LEG-10-NEGRO-L"));
        v.setPrecio(new BigDecimal("64.90"));
        repositorio.saveAndFlush(v);

        assertThat(repositorio.findById(v.getId()).orElseThrow().getPrecio()).isEqualByComparingTo("64.90");
    }

    @Test
    @DisplayName("CA-0a-06 VarianteRepository elimina físicamente una variante")
    void eliminaFisicamente() {
        Variante v = repositorio.saveAndFlush(Datos.variante(producto.getId(), m.getId(), azul.getId(), "LEG-10-AZUL-M"));

        repositorio.deleteById(v.getId());
        repositorio.flush();

        assertThat(repositorio.findById(v.getId())).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 VarianteRepository elimina de forma lógica desactivando la variante")
    void eliminaLogicamente() {
        Variante v = repositorio.saveAndFlush(Datos.variante(producto.getId(), m.getId(), azul.getId(), "LEG-10-AZUL-M2"));
        v.setActivo(false);
        repositorio.saveAndFlush(v);

        assertThat(repositorio.findById(v.getId()).orElseThrow().isActivo()).isFalse();
    }

    @Test
    @DisplayName("CA-0a-06 VarianteRepository lista las variantes de un producto y busca por SKU")
    void listaPorProducto() {
        Producto otro = productos.saveAndFlush(Datos.producto("LEG-11", "Otro", "otro-v", "10.00"));
        Variante v1 = repositorio.saveAndFlush(Datos.variante(producto.getId(), m.getId(), negro.getId(), "LEG-10-NEGRO-M"));
        repositorio.saveAndFlush(Datos.variante(otro.getId(), m.getId(), negro.getId(), "LEG-11-NEGRO-M"));

        assertThat(repositorio.findByProductoIdOrderByIdAsc(producto.getId())).extracting(Variante::getId)
                .containsExactly(v1.getId());
        assertThat(repositorio.findBySku("LEG-11-NEGRO-M")).isPresent();
    }

    @Test
    @DisplayName("CA-0a-06 VarianteRepository rechaza la combinación producto, talla y color duplicada")
    void combinacionDuplicada() {
        repositorio.saveAndFlush(Datos.variante(producto.getId(), m.getId(), negro.getId(), "LEG-10-NEGRO-M"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(
                Datos.variante(producto.getId(), m.getId(), negro.getId(), "LEG-10-OTRO-SKU")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 VarianteRepository rechaza un SKU duplicado")
    void skuDuplicado() {
        repositorio.saveAndFlush(Datos.variante(producto.getId(), m.getId(), negro.getId(), "SKU-REPETIDO"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(
                Datos.variante(producto.getId(), l.getId(), azul.getId(), "SKU-REPETIDO")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
