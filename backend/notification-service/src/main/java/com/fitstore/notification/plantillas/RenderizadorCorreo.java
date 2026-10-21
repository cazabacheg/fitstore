package com.fitstore.notification.plantillas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Arma cada correo con dos capas (micro D2): el diseño y los bloques dinámicos viven en Thymeleaf
 * ({@code templates/email}) y los textos vienen de la tabla {@code plantilla} con sustitución simple de variables.
 * Devuelve el HTML con la marca de la tienda y su versión en texto plano.
 */
@Component
public class RenderizadorCorreo {

    private static final Locale ES_PE = Locale.forLanguageTag("es-PE");
    private static final String[][] POLITICAS = {
        {"Términos y condiciones", "terminos"}, {"Privacidad", "privacidad"},
        {"Cambios y devoluciones", "cambios-devoluciones"}, {"Envíos", "envios"}};

    private final TemplateEngine motor;

    public RenderizadorCorreo(TemplateEngine motor) {
        this.motor = motor;
    }

    @SuppressWarnings("unchecked")
    public CorreoRenderizado renderizar(CatalogoPlantillas plantilla, TextosPlantilla textos,
            Map<String, Object> variables, MarcaVista marca) {
        Map<String, Object> vars = new HashMap<>(variables == null ? Map.of() : variables);
        vars.put("nombreTienda", marca.nombre());

        String asunto = SustitucionVariables.sustituir(textos.asunto(), vars);
        String titulo = SustitucionVariables.sustituir(textos.titulo(), vars);
        String mensajeHtml = SustitucionVariables.mensajeAHtml(textos.mensaje(), vars);
        String mensajeTexto = SustitucionVariables.mensajeATexto(textos.mensaje(), vars);
        String botonTexto = textos.textoBoton() == null || textos.textoBoton().isBlank() ? null
                : SustitucionVariables.sustituir(textos.textoBoton(), vars);
        String botonUrl = enlaceSeguro(vars.get(VariablesCorreo.ENLACE_BOTON));
        if (botonTexto == null) {
            botonUrl = null;
        }
        List<Map<String, Object>> filas = lista(vars.get(VariablesCorreo.FILAS));
        List<Map<String, Object>> items = lista(vars.get(VariablesCorreo.ITEMS));
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("subtotal", vars.get("subtotal"));
        resumen.put("descuento", vars.get("descuento"));
        resumen.put("costoEnvio", vars.get("costoEnvio"));
        resumen.put("total", vars.get("total"));
        boolean sinBoleta = Boolean.TRUE.equals(vars.get(VariablesCorreo.SIN_BOLETA));

        Context contexto = new Context(ES_PE);
        contexto.setVariable("asunto", asunto);
        contexto.setVariable("m", comoMapa(marca));
        contexto.setVariable("titulo", titulo);
        contexto.setVariable("mensajeHtml", mensajeHtml);
        contexto.setVariable("filas", filas);
        contexto.setVariable("items", items);
        contexto.setVariable("resumen", resumen);
        contexto.setVariable("sinBoleta", sinBoleta);
        contexto.setVariable("vista", plantilla.vista());
        contexto.setVariable("botonTexto", botonTexto);
        contexto.setVariable("botonUrl", botonUrl);
        contexto.setVariable("politicas", politicas(marca));
        String html = motor.process("email/layout", contexto);

        String texto = textoPlano(marca, titulo, mensajeTexto, filas, items, resumen, sinBoleta, botonTexto, botonUrl);
        return new CorreoRenderizado(asunto, html, texto);
    }

    private static String textoPlano(MarcaVista marca, String titulo, String mensaje, List<Map<String, Object>> filas,
            List<Map<String, Object>> items, Map<String, Object> resumen, boolean sinBoleta, String botonTexto,
            String botonUrl) {
        StringBuilder t = new StringBuilder();
        t.append(marca.nombre()).append("\n\n").append(titulo).append("\n\n").append(mensaje).append("\n");
        if (!filas.isEmpty()) {
            t.append("\n");
            filas.forEach(f -> t.append(f.get("etiqueta")).append(": ").append(f.get("valor")).append("\n"));
        }
        if (!items.isEmpty()) {
            t.append("\nTu pedido:\n");
            items.forEach(i -> t.append("- ").append(i.get("producto")).append(" (").append(i.get("talla")).append(", ")
                    .append(i.get("color")).append(") x").append(i.get("cantidad")).append(" - ").append(i.get("total"))
                    .append("\n"));
            resumen.forEach((clave, valor) -> {
                if (valor != null) {
                    t.append(etiquetaResumen(clave)).append(": ").append(valor).append("\n");
                }
            });
        }
        if (sinBoleta) {
            t.append("\nPor ahora no pudimos adjuntar la boleta: tu boleta estará disponible en Mis pedidos.\n");
        }
        if (botonTexto != null && botonUrl != null) {
            t.append("\n").append(botonTexto).append(": ").append(botonUrl).append("\n");
        }
        t.append("\n--\n").append(marca.nombre()).append("\n");
        agregar(t, marca.direccion());
        agregar(t, marca.horario());
        agregar(t, marca.email());
        agregar(t, marca.telefono());
        for (String[] politica : POLITICAS) {
            t.append(politica[0]).append(": ").append(marca.urlTienda()).append("/politicas/").append(politica[1]).append("\n");
        }
        return t.toString();
    }

    private static String etiquetaResumen(String clave) {
        return switch (clave) {
            case "subtotal" -> "Subtotal";
            case "descuento" -> "Descuento";
            case "costoEnvio" -> "Envío";
            default -> "Total";
        };
    }

    private static void agregar(StringBuilder t, String linea) {
        if (linea != null && !linea.isBlank()) {
            t.append(linea).append("\n");
        }
    }

    private static List<Map<String, String>> politicas(MarcaVista marca) {
        List<Map<String, String>> enlaces = new ArrayList<>();
        for (String[] politica : POLITICAS) {
            enlaces.add(Map.of("texto", politica[0], "url", marca.urlTienda() + "/politicas/" + politica[1]));
        }
        return enlaces;
    }

    /** Solo se enlazan URLs http(s); cualquier otro esquema (por ejemplo {@code javascript:}) se descarta. */
    private static String enlaceSeguro(Object valor) {
        if (valor instanceof String url && (url.startsWith("https://") || url.startsWith("http://"))) {
            return url;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lista(Object valor) {
        return valor instanceof List<?> lista ? (List<Map<String, Object>>) lista : List.of();
    }

    private static Map<String, Object> comoMapa(MarcaVista marca) {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", marca.nombre());
        m.put("logoUrl", marca.logoUrl());
        m.put("colorPrimario", marca.colorPrimario());
        m.put("colorSecundario", marca.colorSecundario());
        m.put("colorFondo", marca.colorFondo());
        m.put("colorTexto", marca.colorTexto());
        m.put("colorSobrePrimario", marca.colorSobrePrimario());
        m.put("colorSobreSecundario", marca.colorSobreSecundario());
        m.put("email", marca.email());
        m.put("telefono", marca.telefono());
        m.put("horario", marca.horario());
        m.put("direccion", marca.direccion());
        m.put("instagramUrl", marca.instagramUrl());
        m.put("facebookUrl", marca.facebookUrl());
        m.put("tiktokUrl", marca.tiktokUrl());
        return m;
    }
}
