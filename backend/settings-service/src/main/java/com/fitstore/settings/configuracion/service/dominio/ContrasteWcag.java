package com.fitstore.settings.configuracion.service.dominio;

/** Contraste WCAG 2.x entre dos colores (RNF-19). Pendiente de implementar. */
public final class ContrasteWcag {

    public static final double MINIMO_AA = 4.5;

    private ContrasteWcag() {
    }

    public static double razon(String hex1, String hex2) {
        throw new UnsupportedOperationException("pendiente");
    }

    public static boolean cumpleAA(double razon) {
        throw new UnsupportedOperationException("pendiente");
    }

    public static boolean textoLegibleSobrePrimario(String primario, String colorTexto) {
        throw new UnsupportedOperationException("pendiente");
    }
}
