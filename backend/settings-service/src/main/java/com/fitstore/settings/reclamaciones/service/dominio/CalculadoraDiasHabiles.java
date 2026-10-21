package com.fitstore.settings.reclamaciones.service.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Suma días hábiles (lunes a viernes) para la fecha límite de los reclamos. Los feriados no se descuentan en
 * esta versión (micro de settings, A2). El día de partida no cuenta: el primer día hábil es el siguiente.
 */
public final class CalculadoraDiasHabiles {

    private CalculadoraDiasHabiles() {
    }

    public static LocalDate sumar(LocalDate desde, int dias) {
        LocalDate fecha = desde;
        int pendientes = dias;
        while (pendientes > 0) {
            fecha = fecha.plusDays(1);
            if (esHabil(fecha)) {
                pendientes--;
            }
        }
        return fecha;
    }

    private static boolean esHabil(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
    }
}
