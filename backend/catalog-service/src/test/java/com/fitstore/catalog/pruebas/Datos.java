package com.fitstore.catalog.pruebas;

import com.fitstore.catalog.catalogo.model.Categoria;
import com.fitstore.catalog.catalogo.model.Imagen;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.TramoDescuento;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.inventario.model.Stock;
import java.math.BigDecimal;

/** Fábricas de entidades válidas para las pruebas. */
public final class Datos {

    private Datos() {
    }

    public static Categoria categoria(String nombre, String slug, Long padreId) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setSlug(slug);
        c.setOrden(1);
        c.setPadreId(padreId);
        return c;
    }

    public static Producto producto(String codigo, String nombre, String slug, String precio) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setSlug(slug);
        p.setDescripcion("Prenda deportiva de Gamarra");
        p.setPrecioBase(new BigDecimal(precio));
        p.setPrecioDesde(new BigDecimal(precio));
        return p;
    }

    public static Variante variante(Long productoId, Long tallaId, Long colorId, String sku) {
        Variante v = new Variante();
        v.setProductoId(productoId);
        v.setTallaId(tallaId);
        v.setColorId(colorId);
        v.setSku(sku);
        return v;
    }

    public static Stock stock(Long varianteId, int fisico, int reservado) {
        Stock s = new Stock();
        s.setVarianteId(varianteId);
        s.setFisico(fisico);
        s.setReservado(reservado);
        return s;
    }

    public static TramoDescuento tramo(Long productoId, int unidades, String porcentaje) {
        TramoDescuento t = new TramoDescuento();
        t.setProductoId(productoId);
        t.setUnidadesMinimas(unidades);
        t.setPorcentaje(new BigDecimal(porcentaje));
        return t;
    }

    public static Imagen imagen(Long productoId, Long colorId, String clave, boolean principal) {
        Imagen i = new Imagen();
        i.setProductoId(productoId);
        i.setColorId(colorId);
        i.setClave(clave);
        i.setOrden(1);
        i.setPrincipal(principal);
        i.setAncho(1200);
        i.setAlto(1200);
        return i;
    }
}
