package com.fitstore.catalog.catalogo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.TramoDescuento;
import com.fitstore.catalog.pruebas.Datos;
import com.fitstore.catalog.pruebas.PruebaRepositorio;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@PruebaRepositorio
class TramoDescuentoRepositoryTest {

    @Autowired TramoDescuentoRepository repositorio;
    @Autowired ProductoRepository productos;

    Producto producto;

    @BeforeEach
    void producto() {
        producto = productos.saveAndFlush(Datos.producto("TOP-01", "Top Flex", "top-flex", "39.90"));
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository tiene sembrada la política global 3, 6 y 12 unidades")
    void politicaGlobalSemilla() {
        assertThat(repositorio.findByProductoIdIsNullOrderByUnidadesMinimasAsc())
                .extracting(TramoDescuento::getUnidadesMinimas, t -> t.getPorcentaje().intValue())
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3, 5),
                        org.assertj.core.groups.Tuple.tuple(6, 10), org.assertj.core.groups.Tuple.tuple(12, 15));
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository inserta un tramo global y uno por producto")
    void insertaGlobalYPorProducto() {
        TramoDescuento global = repositorio.saveAndFlush(Datos.tramo(null, 24, "20.00"));
        TramoDescuento propio = repositorio.saveAndFlush(Datos.tramo(producto.getId(), 2, "3.50"));

        assertThat(repositorio.findById(global.getId()).orElseThrow().getProductoId()).isNull();
        assertThat(repositorio.findById(propio.getId()).orElseThrow().getPorcentaje()).isEqualByComparingTo("3.50");
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository actualiza unidades y porcentaje")
    void actualiza() {
        TramoDescuento t = repositorio.saveAndFlush(Datos.tramo(producto.getId(), 2, "3.00"));
        t.setUnidadesMinimas(4);
        t.setPorcentaje(new BigDecimal("7.50"));
        repositorio.saveAndFlush(t);

        TramoDescuento leido = repositorio.findById(t.getId()).orElseThrow();
        assertThat(leido.getUnidadesMinimas()).isEqualTo(4);
        assertThat(leido.getPorcentaje()).isEqualByComparingTo("7.50");
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository elimina un tramo")
    void elimina() {
        TramoDescuento t = repositorio.saveAndFlush(Datos.tramo(producto.getId(), 2, "3.00"));

        repositorio.deleteById(t.getId());
        repositorio.flush();

        assertThat(repositorio.findById(t.getId())).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository lista los tramos de un producto ordenados por unidades")
    void listaPorProducto() {
        repositorio.saveAndFlush(Datos.tramo(producto.getId(), 10, "12.00"));
        repositorio.saveAndFlush(Datos.tramo(producto.getId(), 2, "3.00"));

        assertThat(repositorio.findByProductoIdOrderByUnidadesMinimasAsc(producto.getId()))
                .extracting(TramoDescuento::getUnidadesMinimas).containsExactly(2, 10);
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository rechaza unidades mínimas repetidas en un producto")
    void unidadesRepetidasPorProducto() {
        repositorio.saveAndFlush(Datos.tramo(producto.getId(), 2, "3.00"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.tramo(producto.getId(), 2, "4.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository rechaza unidades mínimas repetidas en la política global (NULLS NOT DISTINCT)")
    void unidadesRepetidasGlobales() {
        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.tramo(null, 3, "9.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository rechaza un porcentaje mayor a 50 (CHECK)")
    void porcentajeMayorA50() {
        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.tramo(producto.getId(), 2, "50.01")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository rechaza un porcentaje negativo (CHECK)")
    void porcentajeNegativo() {
        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.tramo(producto.getId(), 2, "-1.00")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 TramoDescuentoRepository acepta los extremos 0 y 50")
    void extremosValidos() {
        repositorio.saveAndFlush(Datos.tramo(producto.getId(), 2, "0.00"));
        TramoDescuento maximo = repositorio.saveAndFlush(Datos.tramo(producto.getId(), 3, "50.00"));

        assertThat(maximo.getId()).isNotNull();
    }
}
