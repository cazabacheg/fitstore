package com.fitstore.catalog.catalogo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.pruebas.Datos;
import com.fitstore.catalog.pruebas.PruebaRepositorio;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class CategoriaRepositoryTest {

    @Autowired CategoriaRepository repositorio;
    @Autowired ProductoRepository productos;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository inserta una raíz y una hija con su auditoría")
    void insertaRaizEHija() {
        Categoria raiz = repositorio.saveAndFlush(Datos.categoria("Polos y tops", "polos-y-tops", null));
        Categoria hija = repositorio.saveAndFlush(Datos.categoria("Polos", "polos", raiz.getId()));

        Categoria leida = repositorio.findById(hija.getId()).orElseThrow();
        assertThat(leida.getPadreId()).isEqualTo(raiz.getId());
        assertThat(leida.isActivo()).isTrue();
        assertThat(leida.getCreadoEn()).isNotNull();
        assertThat(leida.getActualizadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository actualiza nombre y orden")
    void actualiza() {
        Categoria c = repositorio.saveAndFlush(Datos.categoria("Conjuntos", "conjuntos", null));
        c.setNombre("Conjuntos deportivos");
        c.setOrden(7);
        repositorio.saveAndFlush(c);

        Categoria leida = repositorio.findById(c.getId()).orElseThrow();
        assertThat(leida.getNombre()).isEqualTo("Conjuntos deportivos");
        assertThat(leida.getOrden()).isEqualTo(7);
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository elimina físicamente una categoría sin hijas ni productos")
    void eliminaFisicamente() {
        Categoria c = repositorio.saveAndFlush(Datos.categoria("Vestidos", "vestidos", null));

        repositorio.deleteById(c.getId());
        repositorio.flush();

        assertThat(repositorio.findById(c.getId())).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository no permite eliminar una categoría que tiene hijas")
    void noEliminaConHijas() {
        Categoria raiz = repositorio.saveAndFlush(Datos.categoria("Leggings y pantalones", "leggings-y-pantalones", null));
        repositorio.saveAndFlush(Datos.categoria("Leggings", "leggings", raiz.getId()));

        assertThatThrownBy(() -> {
            repositorio.deleteById(raiz.getId());
            repositorio.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository no permite eliminar una categoría con productos asignados")
    void noEliminaConProductos() {
        Categoria c = repositorio.saveAndFlush(Datos.categoria("Casacas", "casacas", null));
        var p = Datos.producto("CAS-01", "Casaca Pro", "casaca-pro", "120.00");
        p.getCategorias().add(c);
        productos.saveAndFlush(p);

        // directo en la base: la FK de producto_categoria es la que lo impide
        assertThatThrownBy(() -> jdbc.update("DELETE FROM categoria WHERE id = ?", c.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository lista el árbol ordenado por orden y nombre")
    void listaOrdenado() {
        Categoria b = Datos.categoria("Bbb", "bbb", null);
        b.setOrden(2);
        Categoria a = Datos.categoria("Aaa", "aaa", null);
        a.setOrden(2);
        Categoria primera = Datos.categoria("Zzz", "zzz", null);
        primera.setOrden(1);
        repositorio.saveAllAndFlush(List.of(b, a, primera));

        List<Categoria> raices = repositorio.findByPadreIdIsNullOrderByOrdenAscNombreAsc();

        assertThat(raices).extracting(Categoria::getNombre).containsSubsequence("Zzz", "Aaa", "Bbb");
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository lista las hijas de una categoría y calcula padre más hijas")
    void listaHijas() {
        Categoria raiz = repositorio.saveAndFlush(Datos.categoria("Partes de abajo", "partes-de-abajo", null));
        Categoria h1 = repositorio.saveAndFlush(Datos.categoria("Leggings", "leggings-h", raiz.getId()));
        Categoria h2 = repositorio.saveAndFlush(Datos.categoria("Shorts", "shorts-h", raiz.getId()));

        assertThat(repositorio.findByPadreIdOrderByOrdenAscNombreAsc(raiz.getId()))
                .extracting(Categoria::getId).containsExactlyInAnyOrder(h1.getId(), h2.getId());
        assertThat(repositorio.idsConHijas(raiz.getId())).containsExactlyInAnyOrder(raiz.getId(), h1.getId(), h2.getId());
        assertThat(repositorio.idsConHijas(h1.getId())).containsExactly(h1.getId());
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository rechaza un nombre duplicado entre hermanas sin importar mayúsculas")
    void nombreDuplicadoEntreHermanas() {
        Categoria raiz = repositorio.saveAndFlush(Datos.categoria("Polos y tops", "polos-tops-x", null));
        repositorio.saveAndFlush(Datos.categoria("Polos", "polos-x", raiz.getId()));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.categoria("POLOS", "polos-y", raiz.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository rechaza dos raíces con el mismo nombre (NULLS NOT DISTINCT)")
    void nombreDuplicadoEntreRaices() {
        repositorio.saveAndFlush(Datos.categoria("Vestidos", "vestidos-a", null));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.categoria("vestidos", "vestidos-b", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository permite el mismo nombre bajo padres distintos")
    void mismoNombreBajoOtroPadre() {
        Categoria p1 = repositorio.saveAndFlush(Datos.categoria("Padre uno", "padre-uno", null));
        Categoria p2 = repositorio.saveAndFlush(Datos.categoria("Padre dos", "padre-dos", null));
        repositorio.saveAndFlush(Datos.categoria("Joggers", "joggers-1", p1.getId()));

        Categoria otra = repositorio.saveAndFlush(Datos.categoria("Joggers", "joggers-2", p2.getId()));

        assertThat(otra.getId()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 CategoriaRepository rechaza un slug duplicado")
    void slugDuplicado() {
        repositorio.saveAndFlush(Datos.categoria("Uno", "slug-repetido", null));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.categoria("Dos", "slug-repetido", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
