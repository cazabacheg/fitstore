package com.fitstore.catalog.catalogo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.ModoDescuento;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.pruebas.Datos;
import com.fitstore.catalog.pruebas.PruebaRepositorio;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class ProductoRepositoryTest {

    @Autowired ProductoRepository repositorio;
    @Autowired CategoriaRepository categorias;
    @Autowired VarianteRepository variantes;
    @Autowired TallaRepository tallas;
    @Autowired ColorRepository colores;
    @Autowired JdbcTemplate jdbc;

    Categoria abajo;
    Categoria leggings;
    Categoria shorts;
    Categoria arriba;

    @BeforeEach
    void arbol() {
        abajo = categorias.saveAndFlush(Datos.categoria("Partes de abajo", "partes-de-abajo", null));
        leggings = categorias.saveAndFlush(Datos.categoria("Leggings", "leggings", abajo.getId()));
        shorts = categorias.saveAndFlush(Datos.categoria("Shorts", "shorts", abajo.getId()));
        arriba = categorias.saveAndFlush(Datos.categoria("Polos y tops", "polos-y-tops", null));
    }

    private Producto guardar(String codigo, String nombre, String slug, String precio, Categoria... cats) {
        Producto p = Datos.producto(codigo, nombre, slug, precio);
        p.getCategorias().addAll(List.of(cats));
        return repositorio.saveAndFlush(p);
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository inserta un producto en BORRADOR con sus categorías y auditoría")
    void inserta() {
        Producto p = guardar("LEG-01", "Legging Pro", "legging-pro", "59.90", leggings);

        Producto leido = repositorio.findById(p.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoProducto.BORRADOR);
        assertThat(leido.getModoDescuento()).isEqualTo(ModoDescuento.GLOBAL);
        assertThat(leido.getPrecioBase()).isEqualByComparingTo("59.90");
        assertThat(leido.getCategorias()).extracting(Categoria::getId).containsExactly(leggings.getId());
        assertThat(leido.getCreadoEn()).isNotNull();
        assertThat(leido.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository actualiza precio y estado")
    void actualiza() {
        Producto p = guardar("LEG-02", "Legging Flex", "legging-flex", "49.90", leggings);
        p.setPrecioBase(new BigDecimal("54.90"));
        p.setEstado(EstadoProducto.ACTIVO);
        repositorio.saveAndFlush(p);

        Producto leido = repositorio.findBySlug("legging-flex").orElseThrow();
        assertThat(leido.getPrecioBase()).isEqualByComparingTo("54.90");
        assertThat(leido.getEstado()).isEqualTo(EstadoProducto.ACTIVO);
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository elimina físicamente un borrador y su asignación a categorías")
    void eliminaFisicamente() {
        Producto p = guardar("BOR-01", "Borrador", "borrador", "10.00", arriba);

        repositorio.deleteById(p.getId());
        repositorio.flush();

        assertThat(repositorio.findById(p.getId())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM producto_categoria WHERE producto_id = ?",
                Integer.class, p.getId())).isZero();
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository elimina de forma lógica pasando el producto a INACTIVO")
    void eliminaLogicamente() {
        Producto p = guardar("ACT-01", "Activo", "activo", "10.00", arriba);
        p.setEstado(EstadoProducto.INACTIVO);
        repositorio.saveAndFlush(p);

        assertThat(repositorio.findByEstado(EstadoProducto.INACTIVO)).extracting(Producto::getId).contains(p.getId());
        assertThat(repositorio.findById(p.getId())).isPresent();
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository lista por categoría padre incluyendo hijas y sin repetir productos")
    void listaPorCategoriaConHijas() {
        Producto a = guardar("A-1", "Legging A", "legging-a", "50.00", abajo, leggings); // padre e hija a la vez
        Producto b = guardar("B-1", "Short B", "short-b", "30.00", shorts);
        guardar("C-1", "Polo C", "polo-c", "25.00", arriba);

        List<Producto> resultado = repositorio.findAll(
                ProductoSpecifications.deCategorias(categorias.idsConHijas(abajo.getId())));

        assertThat(resultado).extracting(Producto::getId).containsExactlyInAnyOrder(a.getId(), b.getId());
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository filtra por talla y color de variantes activas")
    void filtraPorTallaYColor() {
        Talla m = tallas.findByCodigo("M").orElseThrow();
        Talla l = tallas.findByCodigo("L").orElseThrow();
        Color negro = colores.findByNombreIgnoreCase("negro").orElseThrow();
        Color rojo = colores.findByNombreIgnoreCase("rojo").orElseThrow();
        Producto a = guardar("A-2", "Legging A", "legging-a2", "50.00", leggings);
        Producto b = guardar("B-2", "Legging B", "legging-b2", "50.00", leggings);
        variantes.saveAndFlush(Datos.variante(a.getId(), m.getId(), negro.getId(), "A-2-NEGRO-M"));
        variantes.saveAndFlush(Datos.variante(b.getId(), l.getId(), rojo.getId(), "B-2-ROJO-L"));
        var inactiva = Datos.variante(b.getId(), m.getId(), negro.getId(), "B-2-NEGRO-M");
        inactiva.setActivo(false);
        variantes.saveAndFlush(inactiva);

        assertThat(repositorio.findAll(ProductoSpecifications.conTalla(m.getId())))
                .extracting(Producto::getId).containsExactly(a.getId());
        assertThat(repositorio.findAll(ProductoSpecifications.conColor(rojo.getId())))
                .extracting(Producto::getId).containsExactly(b.getId());
        assertThat(repositorio.findAll(Specification.allOf(
                ProductoSpecifications.conTalla(l.getId()), ProductoSpecifications.conColor(rojo.getId()))))
                .extracting(Producto::getId).containsExactly(b.getId());
        assertThat(repositorio.findAll(ProductoSpecifications.conColor(negro.getId())))
                .extracting(Producto::getId).containsExactly(a.getId());
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository filtra por rango de precio desde")
    void filtraPorPrecio() {
        Producto barato = guardar("P-1", "Barato", "barato", "20.00", arriba);
        Producto medio = guardar("P-2", "Medio", "medio", "60.00", arriba);
        guardar("P-3", "Caro", "caro", "150.00", arriba);

        List<Producto> resultado = repositorio.findAll(
                ProductoSpecifications.precioEntre(new BigDecimal("10"), new BigDecimal("100")));

        assertThat(resultado).extracting(Producto::getId).containsExactlyInAnyOrder(barato.getId(), medio.getId());
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository busca por texto sin tildes ni mayúsculas")
    void buscaSinTildes() {
        Producto p = guardar("T-1", "Pólo Algodón Peruano", "polo-algodon-peruano", "35.00", arriba);
        guardar("T-2", "Short Running", "short-running", "30.00", shorts);

        assertThat(repositorio.findAll(ProductoSpecifications.textoContiene("polo algodon")))
                .extracting(Producto::getId).containsExactly(p.getId());
        assertThat(repositorio.findAll(ProductoSpecifications.textoContiene("PÓLO")))
                .extracting(Producto::getId).containsExactly(p.getId());
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository rechaza un slug duplicado")
    void slugDuplicado() {
        guardar("S-1", "Uno", "slug-igual", "10.00", arriba);

        assertThatThrownBy(() -> guardar("S-2", "Dos", "slug-igual", "10.00", arriba))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository rechaza un código duplicado")
    void codigoDuplicado() {
        guardar("COD-1", "Uno", "uno-cod", "10.00", arriba);

        assertThatThrownBy(() -> guardar("COD-1", "Dos", "dos-cod", "10.00", arriba))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository guarda el estado y el modo de descuento como texto del micro")
    void guardaEnumsComoTexto() {
        Producto p = guardar("E-1", "Enum", "enum-prod", "10.00", arriba);
        p.setEstado(EstadoProducto.ACTIVO);
        p.setModoDescuento(ModoDescuento.SIN_DESCUENTO);
        repositorio.saveAndFlush(p);

        assertThat(jdbc.queryForMap("SELECT estado, modo_descuento FROM producto WHERE id = ?", p.getId()))
                .containsEntry("estado", "ACTIVO").containsEntry("modo_descuento", "SIN_DESCUENTO");
    }

    @Test
    @DisplayName("CA-0a-06 ProductoRepository rechaza un estado fuera de BORRADOR, ACTIVO e INACTIVO (CHECK)")
    void estadoInvalido() {
        Producto p = guardar("E-2", "Enum", "enum-prod-2", "10.00", arriba);

        assertThatThrownBy(() -> jdbc.update("UPDATE producto SET estado = 'OTRO' WHERE id = ?", p.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
