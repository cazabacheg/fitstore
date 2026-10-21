package com.fitstore.catalog.catalogo.service.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.catalog.catalogo.model.ModoDescuento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** La función pura de descuentos (micro 4.5 y 9): borde de tramos, surtido, modos, interruptor y redondeo. */
class CalculadoraDescuentosTest {

    private static final List<Tramo> GLOBAL = List.of(
            new Tramo(3, new BigDecimal("5")), new Tramo(6, new BigDecimal("10")), new Tramo(12, new BigDecimal("15")));

    private final CalculadoraDescuentos calculadora = new CalculadoraDescuentos();

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    private static PoliticaDescuentos politica() {
        return new PoliticaDescuentos(true, GLOBAL, Map.of(), Map.of());
    }

    private static LineaCotizable linea(long varianteId, long productoId, String precio, int cantidad) {
        return new LineaCotizable(varianteId, productoId, d(precio), cantidad);
    }

    @Test
    @DisplayName("CA-2-13 el ejemplo del micro: 6 leggings surtidos llevan 10 % (−36.94) y el top no lleva descuento")
    void ejemploDelMicro() {
        Cotizacion c = calculadora.calcular(List.of(
                linea(1, 100, "59.90", 2), linea(2, 100, "59.90", 2), linea(3, 100, "64.90", 2), linea(4, 200, "39.90", 1)),
                politica());

        assertThat(c.lineas().get(0).descuentoMonto()).isEqualByComparingTo("11.98");
        assertThat(c.lineas().get(1).descuentoMonto()).isEqualByComparingTo("11.98");
        assertThat(c.lineas().get(2).descuentoMonto()).isEqualByComparingTo("12.98");
        assertThat(c.lineas().get(0).descuentoPorcentaje()).isEqualByComparingTo("10");
        assertThat(c.lineas().get(3).descuentoMonto()).isEqualByComparingTo("0");
        assertThat(c.lineas().get(3).total()).isEqualByComparingTo("39.90");
        assertThat(c.subtotal()).isEqualByComparingTo("409.30");
        assertThat(c.descuentoTotal()).isEqualByComparingTo("36.94");
        assertThat(c.total()).isEqualByComparingTo("372.36");
    }

    @Test
    @DisplayName("CA-2-13 el tramo se alcanza justo en el borde: 2 sin descuento, 3 con 5 %, 5 con 5 %, 6 con 10 %, 12 con 15 %")
    void bordesDeLosTramos() {
        assertThat(porcentajeCon(2)).isEqualByComparingTo("0");
        assertThat(porcentajeCon(3)).isEqualByComparingTo("5");
        assertThat(porcentajeCon(5)).isEqualByComparingTo("5");
        assertThat(porcentajeCon(6)).isEqualByComparingTo("10");
        assertThat(porcentajeCon(11)).isEqualByComparingTo("10");
        assertThat(porcentajeCon(12)).isEqualByComparingTo("15");
        assertThat(porcentajeCon(40)).isEqualByComparingTo("15");
    }

    private BigDecimal porcentajeCon(int unidades) {
        return calculadora.calcular(List.of(linea(1, 100, "50.00", unidades)), politica()).lineas().get(0).descuentoPorcentaje();
    }

    @Test
    @DisplayName("CA-2-13 el redondeo es half-up por línea, no sobre el total")
    void redondeoHalfUpPorLinea() {
        // 3 líneas de 33.33 (3 unidades en total, 5 %): cada línea 1.6665 → 1.67, total 5.01 (no 4.9995 → 5.00)
        Cotizacion c = calculadora.calcular(
                List.of(linea(1, 100, "33.33", 1), linea(2, 100, "33.33", 1), linea(3, 100, "33.33", 1)), politica());

        assertThat(c.lineas()).allSatisfy(l -> assertThat(l.descuentoMonto()).isEqualByComparingTo("1.67"));
        assertThat(c.descuentoTotal()).isEqualByComparingTo("5.01");
        assertThat(c.total()).isEqualByComparingTo("94.98");
    }

    @Test
    @DisplayName("CA-2-14 indica cuánto falta para el siguiente tramo")
    void cuantoFaltaParaElSiguiente() {
        ResumenProducto cuatro = calculadora.calcular(List.of(linea(1, 100, "50.00", 4)), politica()).productos().get(0);
        assertThat(cuatro.unidades()).isEqualTo(4);
        assertThat(cuatro.tramoAlcanzado().unidadesMinimas()).isEqualTo(3);
        assertThat(cuatro.siguienteTramo().unidadesMinimas()).isEqualTo(6);
        assertThat(cuatro.siguienteTramo().porcentaje()).isEqualByComparingTo("10");
        assertThat(cuatro.faltan()).isEqualTo(2);

        ResumenProducto diez = calculadora.calcular(List.of(linea(1, 100, "50.00", 10)), politica()).productos().get(0);
        assertThat(diez.siguienteTramo().unidadesMinimas()).isEqualTo(12);
        assertThat(diez.faltan()).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-2-14 sin tramo alcanzado el siguiente es el primero; en el último no hay siguiente")
    void extremosDelResumen() {
        ResumenProducto una = calculadora.calcular(List.of(linea(1, 100, "50.00", 1)), politica()).productos().get(0);
        assertThat(una.tramoAlcanzado()).isNull();
        assertThat(una.siguienteTramo().unidadesMinimas()).isEqualTo(3);
        assertThat(una.faltan()).isEqualTo(2);

        ResumenProducto muchas = calculadora.calcular(List.of(linea(1, 100, "50.00", 12)), politica()).productos().get(0);
        assertThat(muchas.tramoAlcanzado().unidadesMinimas()).isEqualTo(12);
        assertThat(muchas.siguienteTramo()).isNull();
        assertThat(muchas.faltan()).isZero();
    }

    @Test
    @DisplayName("CA-2-13 suma las unidades por producto sin mezclar productos distintos")
    void agrupaPorProducto() {
        Cotizacion c = calculadora.calcular(List.of(linea(1, 100, "50.00", 2), linea(2, 200, "50.00", 2)), politica());

        assertThat(c.descuentoTotal()).isEqualByComparingTo("0");
        assertThat(c.productos()).hasSize(2);
    }

    @Test
    @DisplayName("CA-2-13 un producto en modo PERSONALIZADO usa sus tramos y uno SIN_DESCUENTO ninguno")
    void modosPorProducto() {
        PoliticaDescuentos politica = new PoliticaDescuentos(true, GLOBAL,
                Map.of(100L, ModoDescuento.PERSONALIZADO, 200L, ModoDescuento.SIN_DESCUENTO),
                Map.of(100L, List.of(new Tramo(2, new BigDecimal("20")))));

        Cotizacion c = calculadora.calcular(List.of(
                linea(1, 100, "50.00", 2), linea(2, 200, "50.00", 12), linea(3, 300, "50.00", 3)), politica);

        assertThat(c.lineas().get(0).descuentoPorcentaje()).isEqualByComparingTo("20");
        assertThat(c.lineas().get(1).descuentoPorcentaje()).isEqualByComparingTo("0");
        assertThat(c.lineas().get(2).descuentoPorcentaje()).isEqualByComparingTo("5");
        assertThat(c.productos().get(1).siguienteTramo()).isNull();
    }

    @Test
    @DisplayName("CA-2-13 con el interruptor global apagado ninguna cotización lleva descuento")
    void interruptorApagado() {
        PoliticaDescuentos apagada = new PoliticaDescuentos(false, GLOBAL, Map.of(), Map.of());

        Cotizacion c = calculadora.calcular(List.of(linea(1, 100, "50.00", 12)), apagada);

        assertThat(c.descuentoTotal()).isEqualByComparingTo("0");
        assertThat(c.total()).isEqualByComparingTo("600.00");
        assertThat(c.productos().get(0).siguienteTramo()).isNull();
    }

    @Test
    @DisplayName("CA-2-13 sin líneas la cotización es cero")
    void sinLineas() {
        Cotizacion c = calculadora.calcular(List.of(), politica());

        assertThat(c.lineas()).isEmpty();
        assertThat(c.total()).isEqualByComparingTo("0");
    }
}
