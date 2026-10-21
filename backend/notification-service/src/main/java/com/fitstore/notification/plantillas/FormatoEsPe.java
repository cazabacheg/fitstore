package com.fitstore.notification.plantillas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Fechas y montos de los correos en formato es-PE: "jueves 22 de octubre" y "S/ 332.46" (micro 4.4). */
public final class FormatoEsPe {

    private static final Locale ES_PE = Locale.forLanguageTag("es-PE");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES_PE);

    private FormatoEsPe() {
    }

    public static String fecha(LocalDate fecha) {
        return FECHA.format(fecha);
    }

    public static String moneda(BigDecimal monto) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return "S/ " + formato.format(monto);
    }
}
