package com.fitstore.catalog.catalogo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.Color;
import com.fitstore.catalog.catalogo.model.ConfiguracionDescuento;
import com.fitstore.catalog.catalogo.model.GuiaTallas;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Talla;
import com.fitstore.catalog.pruebas.Datos;
import com.fitstore.catalog.pruebas.PruebaRepositorio;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/** Repositorios de apoyo del módulo catálogo: maestros (talla, color), imágenes, guía de tallas y el interruptor de descuentos. */
@PruebaRepositorio
class MaestrosYApoyoRepositoryTest {

    @Autowired TallaRepository tallas;
    @Autowired ColorRepository colores;
    @Autowired ImagenRepository imagenes;
    @Autowired GuiaTallasRepository guias;
    @Autowired ConfiguracionDescuentoRepository configuracionDescuento;
    @Autowired CategoriaRepository categorias;
    @Autowired ProductoRepository productos;

    private Talla talla(String codigo, int orden) {
        Talla t = new Talla();
        t.setCodigo(codigo);
        t.setOrden(orden);
        return t;
    }

    private Color color(String nombre, String hex) {
        Color c = new Color();
        c.setNombre(nombre);
        c.setHex(hex);
        return c;
    }

    @Test
    @DisplayName("CA-0a-06 TallaRepository lista las tallas sembradas ordenadas de XS a XXL")
    void tallasOrdenadas() {
        assertThat(tallas.findAllByOrderByOrdenAsc()).extracting(Talla::getCodigo)
                .containsExactly("XS", "S", "M", "L", "XL", "XXL");
    }

    @Test
    @DisplayName("CA-0a-06 TallaRepository inserta, actualiza y desactiva una talla")
    void tallaCrud() {
        Talla t = tallas.saveAndFlush(talla("XXXL", 7));
        t.setActivo(false);
        tallas.saveAndFlush(t);

        assertThat(tallas.findByCodigo("XXXL").orElseThrow().isActivo()).isFalse();
    }

    @Test
    @DisplayName("CA-0a-06 TallaRepository rechaza un código de talla duplicado")
    void tallaDuplicada() {
        assertThatThrownBy(() -> tallas.saveAndFlush(talla("M", 9))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ColorRepository tiene los 10 colores sembrados con su hex")
    void coloresSembrados() {
        List<Color> todos = colores.findAll();

        assertThat(todos).extracting(Color::getNombre).containsExactlyInAnyOrder("negro", "blanco", "gris",
                "azul marino", "azul", "rojo", "rosado", "verde", "morado", "beige");
        assertThat(colores.findByNombreIgnoreCase("Negro").orElseThrow().getHex()).isEqualTo("#1D1D1F");
        assertThat(colores.findByNombreIgnoreCase("beige").orElseThrow().getHex()).isEqualTo("#D8C7A8");
    }

    @Test
    @DisplayName("CA-0a-06 ColorRepository inserta y actualiza un color")
    void colorCrud() {
        Color c = colores.saveAndFlush(color("turquesa", "#40E0D0"));
        c.setHex("#30D5C8");
        colores.saveAndFlush(c);

        assertThat(colores.findByNombreIgnoreCase("TURQUESA").orElseThrow().getHex()).isEqualTo("#30D5C8");
    }

    @Test
    @DisplayName("CA-0a-06 ColorRepository rechaza un nombre duplicado sin importar mayúsculas")
    void colorDuplicado() {
        assertThatThrownBy(() -> colores.saveAndFlush(color("NEGRO", "#000000")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ColorRepository rechaza un hex que no sea #RRGGBB (CHECK)")
    void colorHexInvalido() {
        assertThatThrownBy(() -> colores.saveAndFlush(color("fucsia", "rosa")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ImagenRepository inserta imágenes por color y lista las de un producto en orden")
    void imagenesPorColor() {
        Producto p = productos.saveAndFlush(Datos.producto("IMG-01", "Top", "top-img", "30.00"));
        Color rojo = colores.findByNombreIgnoreCase("rojo").orElseThrow();
        Imagen segunda = Datos.imagen(p.getId(), rojo.getId(), "productos/1/b-1200.jpg", false);
        segunda.setOrden(2);
        imagenes.saveAndFlush(segunda);
        imagenes.saveAndFlush(Datos.imagen(p.getId(), rojo.getId(), "productos/1/a-1200.jpg", true));

        assertThat(imagenes.findByProductoIdOrderByOrdenAsc(p.getId())).extracting(Imagen::getClave)
                .containsExactly("productos/1/a-1200.jpg", "productos/1/b-1200.jpg");
    }

    @Test
    @DisplayName("CA-0a-06 ImagenRepository permite imágenes sin color y elimina físicamente")
    void imagenSinColorYElimina() {
        Producto p = productos.saveAndFlush(Datos.producto("IMG-02", "Top", "top-img2", "30.00"));
        Imagen i = imagenes.saveAndFlush(Datos.imagen(p.getId(), null, "productos/2/a.jpg", true));

        assertThat(imagenes.findById(i.getId()).orElseThrow().getColorId()).isNull();
        imagenes.deleteById(i.getId());
        imagenes.flush();
        assertThat(imagenes.findById(i.getId())).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 ImagenRepository rechaza una segunda imagen principal del mismo producto")
    void dosPrincipales() {
        Producto p = productos.saveAndFlush(Datos.producto("IMG-03", "Top", "top-img3", "30.00"));
        imagenes.saveAndFlush(Datos.imagen(p.getId(), null, "productos/3/a.jpg", true));

        assertThatThrownBy(() -> imagenes.saveAndFlush(Datos.imagen(p.getId(), null, "productos/3/b.jpg", true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 GuiaTallasRepository guarda la tabla de medidas como jsonb y la lee por categoría")
    void guiaDeTallas() {
        Categoria c = categorias.saveAndFlush(Datos.categoria("Leggings y pantalones", "guia-leggings", null));
        GuiaTallas g = new GuiaTallas();
        g.setCategoriaId(c.getId());
        g.setTabla(Map.of("columnas", List.of("Cintura (cm)", "Cadera (cm)"),
                "filas", List.of(Map.of("talla", "S", "valores", List.of("62-66", "88-92")))));
        g.setNota("Medidas del cuerpo");
        guias.saveAndFlush(g);

        GuiaTallas leida = guias.findByCategoriaId(c.getId()).orElseThrow();
        assertThat(leida.getTabla()).containsKey("columnas");
        assertThat((List<?>) leida.getTabla().get("columnas")).containsExactly("Cintura (cm)", "Cadera (cm)");
        assertThat(leida.getNota()).isEqualTo("Medidas del cuerpo");
    }

    @Test
    @DisplayName("CA-0a-06 GuiaTallasRepository rechaza dos guías para la misma categoría")
    void guiaDuplicada() {
        Categoria c = categorias.saveAndFlush(Datos.categoria("Vestidos", "guia-vestidos", null));
        GuiaTallas g1 = new GuiaTallas();
        g1.setCategoriaId(c.getId());
        g1.setTabla(Map.of("columnas", List.of()));
        guias.saveAndFlush(g1);
        GuiaTallas g2 = new GuiaTallas();
        g2.setCategoriaId(c.getId());
        g2.setTabla(Map.of("columnas", List.of()));

        assertThatThrownBy(() -> guias.saveAndFlush(g2)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ConfiguracionDescuentoRepository tiene la fila única activa y se puede apagar")
    void interruptorDeDescuentos() {
        ConfiguracionDescuento c = configuracionDescuento.findById(1L).orElseThrow();
        assertThat(c.isActivo()).isTrue();

        c.setActivo(false);
        configuracionDescuento.saveAndFlush(c);

        assertThat(configuracionDescuento.findById(1L).orElseThrow().isActivo()).isFalse();
    }

    @Test
    @DisplayName("CA-0a-06 ConfiguracionDescuentoRepository no admite una segunda fila (fila única)")
    void filaUnica() {
        ConfiguracionDescuento otra = new ConfiguracionDescuento();
        otra.setId(2L);
        otra.setActivo(true);

        assertThatThrownBy(() -> configuracionDescuento.saveAndFlush(otra))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
