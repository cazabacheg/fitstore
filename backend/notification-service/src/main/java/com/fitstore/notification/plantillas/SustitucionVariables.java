package com.fitstore.notification.plantillas;

import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.util.HtmlUtils;

/**
 * Sustitución simple de {@code {{variable}}} en los textos que edita el admin (micro 4.6, SPEC-7 N4). Nunca evalúa
 * nada: no hay expresiones, solo reemplazo de texto; y el HTML se escapa antes de dar formato (sin SSTI ni XSS).
 */
public final class SustitucionVariables {

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");
    private static final Pattern NEGRITA = Pattern.compile("\\*\\*(.+?)\\*\\*", Pattern.DOTALL);

    private SustitucionVariables() {
    }

    /** Reemplaza cada variable por su valor tal cual (para el asunto y el título, que no son HTML). */
    public static String sustituir(String texto, Map<String, Object> variables) {
        return reemplazar(texto, variables, UnaryOperator.identity());
    }

    /** Texto plano del mensaje: variables reemplazadas y sin las marcas de negrita. */
    public static String mensajeATexto(String mensaje, Map<String, Object> variables) {
        return sustituir(mensaje, variables).replace("**", "");
    }

    /** Escapa el mensaje y sus valores, y solo después convierte {@code **negrita**} y los saltos de línea. */
    public static String mensajeAHtml(String mensaje, Map<String, Object> variables) {
        String escapado = reemplazar(escapar(mensaje), variables, SustitucionVariables::escapar);
        String conNegrita = NEGRITA.matcher(escapado).replaceAll("<strong>$1</strong>");
        return conNegrita.replace("\r\n", "\n").replace("\n", "<br/>");
    }

    /** Escapa solo lo que es marcado HTML ({@code < > & " '}); las tildes salen tal cual en UTF-8. */
    private static String escapar(String texto) {
        return HtmlUtils.htmlEscape(texto, "UTF-8");
    }

    private static String reemplazar(String texto, Map<String, Object> variables, UnaryOperator<String> formato) {
        if (texto == null) {
            return "";
        }
        Matcher coincidencia = VARIABLE.matcher(texto);
        StringBuilder resultado = new StringBuilder();
        while (coincidencia.find()) {
            Object valor = variables == null ? null : variables.get(coincidencia.group(1));
            String texto1 = valor == null ? "" : formato.apply(String.valueOf(valor));
            coincidencia.appendReplacement(resultado, Matcher.quoteReplacement(texto1));
        }
        coincidencia.appendTail(resultado);
        return resultado.toString();
    }
}
