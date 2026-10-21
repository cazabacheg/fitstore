package com.fitstore.notification.plantillas;

import java.util.Map;

/**
 * Lo que los correos necesitan de la marca de la tienda (copia de la configuración pública de settings): nombre, logo,
 * colores y datos de contacto para el pie. {@code urlTienda} es la base pública para los logos relativos y las políticas.
 */
public record MarcaVista(String nombre, String logoUrl, String colorPrimario, String colorSecundario, String colorFondo,
        String colorTexto, String email, String telefono, String horario, String direccion, String instagramUrl,
        String facebookUrl, String tiktokUrl, String urlTienda) {

    private static final String BLANCO = "#FFFFFF";

    /** Marca FitStore por defecto: se usa si no hay copia y settings no responde (CA-7-17). */
    public static MarcaVista porDefecto(String urlTienda) {
        return new MarcaVista("FitStore", null, "#C9B28C", "#2B2621", "#FAF9F7", "#26221D", null, null, null, null,
                null, null, null, urlTienda);
    }

    /** Arma la marca desde la configuración pública guardada (identidad, tema y contenido); lo que falte usa el valor por defecto. */
    public static MarcaVista de(Map<String, Object> configuracion, String urlTienda) {
        MarcaVista base = porDefecto(urlTienda);
        if (configuracion == null) {
            return base;
        }
        String logo = texto(configuracion, "identidad", "logoClaroUrl");
        if (logo != null && logo.startsWith("/") && urlTienda != null) {
            logo = urlTienda + logo;
        }
        return new MarcaVista(
                otro(texto(configuracion, "identidad", "nombre"), base.nombre()),
                logo,
                otro(texto(configuracion, "tema", "colorPrimario"), base.colorPrimario()),
                otro(texto(configuracion, "tema", "colorSecundario"), base.colorSecundario()),
                otro(texto(configuracion, "tema", "colorFondo"), base.colorFondo()),
                otro(texto(configuracion, "tema", "colorTexto"), base.colorTexto()),
                texto(configuracion, "contenido", "email"),
                texto(configuracion, "contenido", "telefono"),
                texto(configuracion, "contenido", "horario"),
                textoAnidado(configuracion, "contenido", "tiendaFisica", "direccion"),
                texto(configuracion, "contenido", "instagramUrl"),
                texto(configuracion, "contenido", "facebookUrl"),
                texto(configuracion, "contenido", "tiktokUrl"),
                urlTienda);
    }

    /** Blanco o el color de texto, el que contraste más con el primario (mismo cálculo que {@code fs-color-en-primario}). */
    public String colorSobrePrimario() {
        return colorSobre(colorPrimario);
    }

    /** Lo mismo para el pie, que va sobre el color secundario. */
    public String colorSobreSecundario() {
        return colorSobre(colorSecundario);
    }

    private String colorSobre(String fondo) {
        return contraste(fondo, BLANCO) >= contraste(fondo, colorTexto) ? BLANCO : colorTexto;
    }

    private static double contraste(String a, String b) {
        double la = luminancia(a);
        double lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminancia(String hex) {
        String limpio = hex.startsWith("#") ? hex.substring(1) : hex;
        if (limpio.length() == 3) {
            limpio = "" + limpio.charAt(0) + limpio.charAt(0) + limpio.charAt(1) + limpio.charAt(1)
                    + limpio.charAt(2) + limpio.charAt(2);
        }
        double[] canales = new double[3];
        for (int i = 0; i < 3; i++) {
            double valor = Integer.parseInt(limpio.substring(i * 2, i * 2 + 2), 16) / 255.0;
            canales[i] = valor <= 0.03928 ? valor / 12.92 : Math.pow((valor + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * canales[0] + 0.7152 * canales[1] + 0.0722 * canales[2];
    }

    private static String otro(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }

    private static String texto(Map<String, Object> raiz, String seccion, String campo) {
        return textoAnidado(raiz, seccion, campo);
    }

    @SuppressWarnings("unchecked")
    private static String textoAnidado(Map<String, Object> raiz, String... ruta) {
        Object actual = raiz;
        for (String clave : ruta) {
            if (!(actual instanceof Map<?, ?> mapa)) {
                return null;
            }
            actual = ((Map<String, Object>) mapa).get(clave);
        }
        return actual instanceof String valor && !valor.isBlank() ? valor : null;
    }
}
