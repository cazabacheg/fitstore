package com.fitstore.catalog.catalogo.service;

import com.fitstore.catalog.catalogo.dto.PoliticaDescuentosDto;
import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.service.dominio.PoliticaDescuentos;
import java.util.Collection;

/** Política de descuentos por volumen: la global, el modo de cada producto y sus tramos propios (micro 4.5). */
public interface DescuentoService {

    /** Política global (interruptor y tramos); se lee de la caché de 10 minutos. */
    PoliticaDescuentosDto obtenerGlobal();

    /** Política vigente para estos productos, lista para la {@code CalculadoraDescuentos} y para mostrar etiquetas. */
    PoliticaDescuentos politicaPara(Collection<Producto> productos);
}
