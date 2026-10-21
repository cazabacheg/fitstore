package com.fitstore.catalog.pruebas;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.EstadoProducto;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.CategoriaRepository;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.ImagenRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.TramoDescuentoRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.catalog.inventario.repository.StockRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Base de las pruebas de API: MockMvc, repositorios y fábricas de datos que viven dentro de la transacción de la prueba. */
@PruebaApi
public abstract class ApiBase {

    @Autowired protected MockMvc mvc;
    @Autowired protected EntityManager em;
    @Autowired protected JsonMapper json;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected CategoriaRepository categorias;
    @Autowired protected ProductoRepository productos;
    @Autowired protected VarianteRepository variantes;
    @Autowired protected TallaRepository tallas;
    @Autowired protected ColorRepository colores;
    @Autowired protected StockRepository stock;
    @Autowired protected ImagenRepository imagenes;
    @Autowired protected TramoDescuentoRepository tramos;

    protected static String slug(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    protected Categoria categoria(String nombre, Long padreId) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setSlug(slug(nombre) + "-" + System.nanoTime());
        c.setPadreId(padreId);
        c.setOrden(1);
        return categorias.saveAndFlush(c);
    }

    /** Producto con precio base y precio "desde" iguales; el código debe ser único (A-Z0-9). */
    protected Producto producto(String codigo, String nombre, String precio, EstadoProducto estado, Categoria... cats) {
        Producto p = Datos.producto(codigo, nombre, slug(nombre) + "-" + codigo.toLowerCase(), precio);
        p.setEstado(estado);
        p.setCategorias(new HashSet<>(java.util.List.of(cats)));
        return productos.saveAndFlush(p);
    }

    protected Variante variante(Producto producto, String talla, String color, int fisico, int reservado) {
        Variante v = Datos.variante(producto.getId(), tallas.findByCodigo(talla).orElseThrow().getId(),
                colores.findByNombreIgnoreCase(color).orElseThrow().getId(),
                producto.getCodigo() + "-" + color.substring(0, 3).toUpperCase() + "-" + talla);
        v = variantes.saveAndFlush(v);
        stock.saveAndFlush(Datos.stock(v.getId(), fisico, reservado));
        return v;
    }

    protected Imagen imagen(Producto producto, String color, boolean principal) {
        Imagen i = Datos.imagen(producto.getId(), colores.findByNombreIgnoreCase(color).orElseThrow().getId(),
                "productos/" + producto.getId() + "/" + System.nanoTime(), principal);
        return imagenes.saveAndFlush(i);
    }

    protected Long colorId(String nombre) {
        return colores.findByNombreIgnoreCase(nombre).orElseThrow().getId();
    }

    protected Long tallaId(String codigo) {
        return tallas.findByCodigo(codigo).orElseThrow().getId();
    }

    protected BigDecimal dec(String valor) {
        return new BigDecimal(valor);
    }

    /** Cuerpo JSON de la respuesta. */
    protected JsonNode cuerpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }

    /**
     * Manda a la base lo escrito con JPA y vacía la memoria de Hibernate: la prueba y la petición comparten la
     * transacción, y sin esto la petición vería entidades viejas tras una actualización masiva.
     */
    protected void sincronizar() {
        em.flush();
        em.clear();
    }
}
