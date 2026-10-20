package com.fitstore.catalog.catalogo.service.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Descuentos por volumen (micro 4.5). Función pura: sin Spring ni base de datos.
 *
 * <ol>
 *   <li>Las líneas se agrupan por producto y se suman sus unidades (surtido de tallas y colores).</li>
 *   <li>Los tramos del producto los da la {@link PoliticaDescuentos} según su modo y el interruptor global.</li>
 *   <li>Se aplica el tramo más alto alcanzado al precio de cada línea del producto.</li>
 *   <li>El descuento de cada línea se redondea half-up a 2 decimales (no el total).</li>
 * </ol>
 */
public class CalculadoraDescuentos {

    private static final BigDecimal CIEN = new BigDecimal("100");

    public Cotizacion calcular(List<LineaCotizable> lineas, PoliticaDescuentos politica) {
        Map<Long, Integer> unidadesPorProducto = new LinkedHashMap<>();
        for (LineaCotizable l : lineas) {
            unidadesPorProducto.merge(l.productoId(), l.cantidad(), Integer::sum);
        }

        Map<Long, ResumenProducto> resumenes = new LinkedHashMap<>();
        unidadesPorProducto.forEach((productoId, unidades) -> resumenes.put(productoId,
                resumir(productoId, unidades, politica.tramosDe(productoId))));

        List<LineaCalculada> calculadas = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal descuentoTotal = BigDecimal.ZERO;
        for (LineaCotizable l : lineas) {
            Tramo alcanzado = resumenes.get(l.productoId()).tramoAlcanzado();
            BigDecimal porcentaje = alcanzado == null ? BigDecimal.ZERO : alcanzado.porcentaje();
            BigDecimal importe = l.precioUnitario().multiply(BigDecimal.valueOf(l.cantidad())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal descuento = importe.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
            calculadas.add(new LineaCalculada(l.varianteId(), l.productoId(), l.precioUnitario(), l.cantidad(), importe,
                    porcentaje.setScale(2, RoundingMode.HALF_UP), descuento, importe.subtract(descuento)));
            subtotal = subtotal.add(importe);
            descuentoTotal = descuentoTotal.add(descuento);
        }
        return new Cotizacion(calculadas, List.copyOf(resumenes.values()), subtotal, descuentoTotal, subtotal.subtract(descuentoTotal));
    }

    /** Tramo alcanzado (el más alto con {@code unidadesMinimas <= unidades}) y el siguiente, con cuánto falta. */
    private static ResumenProducto resumir(Long productoId, int unidades, List<Tramo> tramos) {
        Tramo alcanzado = null;
        Tramo siguiente = null;
        for (Tramo t : tramos) {
            if (t.unidadesMinimas() <= unidades) {
                alcanzado = t;
            } else if (siguiente == null) {
                siguiente = t;
            }
        }
        return new ResumenProducto(productoId, unidades, alcanzado, siguiente,
                siguiente == null ? 0 : siguiente.unidadesMinimas() - unidades);
    }
}
