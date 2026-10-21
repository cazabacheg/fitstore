package com.fitstore.notification.plantillas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Las variables de un envío ({@code envio_correo.variables}): los valores de texto que usan los textos de la plantilla
 * ({@code {{nombre}}}) más los bloques del diseño: {@code filas} (etiqueta y valor), {@code items} del pedido y el
 * {@code enlaceBoton}. Todo ya viene formateado en es-PE y nunca lleva el DNI (micro 2.2).
 */
public final class VariablesCorreo {

    public static final String FILAS = "filas";
    public static final String ITEMS = "items";
    public static final String ENLACE_BOTON = "enlaceBoton";
    public static final String SIN_BOLETA = "sinBoleta";

    private final Map<String, Object> valores = new LinkedHashMap<>();
    private final List<Map<String, String>> filas = new ArrayList<>();

    private VariablesCorreo() {
    }

    public static VariablesCorreo nuevo() {
        return new VariablesCorreo();
    }

    /** Agrega una variable; si el valor es nulo no la agrega. */
    public VariablesCorreo con(String clave, Object valor) {
        if (valor != null) {
            valores.put(clave, valor);
        }
        return this;
    }

    /** Fila "etiqueta: valor" del bloque de detalle; se omite si no hay valor. */
    public VariablesCorreo fila(String etiqueta, String valor) {
        return filaConEnlace(etiqueta, valor, null);
    }

    /** Fila cuyo valor es un enlace (por ejemplo, el rastreo de la agencia). */
    public VariablesCorreo filaConEnlace(String etiqueta, String valor, String enlace) {
        if (valor != null && !valor.isBlank()) {
            Map<String, String> fila = new LinkedHashMap<>();
            fila.put("etiqueta", etiqueta);
            fila.put("valor", valor);
            if (enlace != null && !enlace.isBlank()) {
                fila.put("enlace", enlace);
            }
            filas.add(fila);
        }
        return this;
    }

    public VariablesCorreo boton(String url) {
        return con(ENLACE_BOTON, url);
    }

    public Map<String, Object> construir() {
        Map<String, Object> resultado = new LinkedHashMap<>(valores);
        if (!filas.isEmpty()) {
            resultado.put(FILAS, new ArrayList<>(filas));
        }
        return resultado;
    }
}
