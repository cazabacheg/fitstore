package com.fitstore.settings.configuracion.service.dominio;

/**
 * Contraste WCAG 2.x entre dos colores (RNF-19). Usa la luminancia relativa; la razón va de 1:1 a 21:1.
 * Solo se usa para advertir: guardar el tema nunca falla por contraste.
 */
public final class ContrasteWcag {

    /** Mínimo de nivel AA para texto normal. */
    public static final double MINIMO_AA = 4.5;

    private static final String BLANCO = "#FFFFFF";

    private ContrasteWcag() {
    }

    /** Razón de contraste entre dos colores {@code #RRGGBB}, siempre mayor o igual a 1. */
    public static double razon(String hex1, String hex2) {
        double l1 = luminancia(hex1);
        double l2 = luminancia(hex2);
        double claro = Math.max(l1, l2);
        double oscuro = Math.min(l1, l2);
        return (claro + 0.05) / (oscuro + 0.05);
    }

    public static boolean cumpleAA(double razon) {
        return razon >= MINIMO_AA;
    }

    /** Verdadero si el blanco o el color de texto se leen sobre el primario con al menos 4.5:1 (CA-3-10). */
    public static boolean textoLegibleSobrePrimario(String primario, String colorTexto) {
        return cumpleAA(razon(primario, BLANCO)) || cumpleAA(razon(primario, colorTexto));
    }

    private static double luminancia(String hex) {
        double r = canal(Integer.parseInt(hex.substring(1, 3), 16));
        double g = canal(Integer.parseInt(hex.substring(3, 5), 16));
        double b = canal(Integer.parseInt(hex.substring(5, 7), 16));
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double canal(int valor) {
        double c = valor / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
}
