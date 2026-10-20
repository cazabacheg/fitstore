package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.DescuentoProductoRequest;
import com.fitstore.catalog.catalogo.dto.PoliticaDescuentosDto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.service.dominio.PoliticaDescuentos;
import java.util.Collection;

/** Política de descuentos por volumen: la global, el modo de cada producto y sus tramos propios (micro 4.5). */
public interface DescuentoService {

    /** Política global (interruptor y tramos); se lee de la caché de 10 minutos. */
    PoliticaDescuentosDto obtenerGlobal();

    /**
     * Guarda la política global: reemplaza los tramos y el interruptor. Exige hasta 10 tramos con unidades y porcentajes
     * crecientes (CA-2-36); invalida la caché de la política y de los detalles de producto (CA-2-37).
     */
    PoliticaDescuentosDto guardarGlobal(PoliticaDescuentosDto politica);

    /**
     * Fija el modo de descuento del producto. En PERSONALIZADO exige entre 1 y 10 tramos válidos y los reemplaza; en los demás
     * modos borra los tramos propios (CA-2-38). Invalida el detalle del producto.
     */
    void configurarProducto(Producto producto, DescuentoProductoRequest peticion);

    /** Política vigente para estos productos, lista para la {@code CalculadoraDescuentos} y para mostrar etiquetas. */
    PoliticaDescuentos politicaPara(Collection<Producto> productos);
}
