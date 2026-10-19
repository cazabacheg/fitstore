package com.fitstore.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Utilidades de {@link BigDecimal} para montos en soles: escala 2 y redondeo half-up. */
public final class Dinero {

    public static final int ESCALA = 2;
    public static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA);

    private Dinero() {
    }

    public static BigDecimal de(String valor) {
        return redondear(new BigDecimal(valor));
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    public static BigDecimal sumar(BigDecimal a, BigDecimal b) {
        return redondear(a.add(b));
    }

    public static BigDecimal restar(BigDecimal a, BigDecimal b) {
        return redondear(a.subtract(b));
    }

    public static BigDecimal multiplicar(BigDecimal monto, int cantidad) {
        return redondear(monto.multiply(BigDecimal.valueOf(cantidad)));
    }

    /** {@code base × porcentaje / 100}, redondeado half-up a 2 decimales. */
    public static BigDecimal porcentaje(BigDecimal base, BigDecimal porcentaje) {
        return redondear(base.multiply(porcentaje).divide(BigDecimal.valueOf(100)));
    }
}
