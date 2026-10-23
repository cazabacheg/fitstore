package com.fitstore.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fitstore.payment.service.BoletaServiceImpl.LineaBoleta;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Diseño del cuadro de totales de la boleta (P-5-06): a la derecha, angosto y con letra menor que la tabla (CA-5-41). */
class BoletaPdfTest {

    private static final float ANCHO_PAGINA = 595.3f;

    /** Un fragmento de texto del PDF con su posición horizontal y el tamaño de su letra. */
    record Linea(String texto, float izquierda, float derecha, float letra) {}

    @Test
    @DisplayName("CA-5-41 el cuadro de totales va a la derecha, ocupa menos de la mitad del ancho y su letra es menor que la de la tabla")
    void cuadroDeTotalesALaDerecha() throws Exception {
        byte[] pdf = generar("5.00");
        String salida = System.getProperty("boleta.salida");
        if (salida != null) {
            Files.write(Path.of(salida), pdf);
        }

        List<Linea> lineas = lineasDe(pdf);
        Linea subtotal = buscar(lineas, "Subtotal");
        Linea envio = buscar(lineas, "Envío");
        Linea total = buscar(lineas, "TOTAL");
        Linea importeTotal = buscar(lineas, "129.80");
        Linea importeTabla = lineas.stream().filter(l -> l.texto().equals("119.80")).findFirst().orElseThrow();
        float margenDerecho = importeTabla.derecha();

        // a la derecha: el importe del total termina donde termina la columna de importes de la tabla
        assertThat(importeTotal.derecha()).isCloseTo(margenDerecho, within(4f));
        // angosto: las etiquetas del cuadro empiezan pasada la mitad de la página
        assertThat(subtotal.izquierda()).isGreaterThan(ANCHO_PAGINA * 0.5f);
        assertThat(envio.izquierda()).isGreaterThan(ANCHO_PAGINA * 0.5f);
        // letra menor que la de los valores de la tabla de ítems
        assertThat(subtotal.letra()).isLessThan(importeTabla.letra());
        assertThat(total.letra()).isLessThan(importeTabla.letra());
        assertThat(importeTotal.letra()).isLessThan(importeTabla.letra());
    }

    @Test
    @DisplayName("CA-5-44 cada valor de la tabla de ítems queda bajo su cabecera: números a la derecha y la cantidad centrada")
    void tablaAlineadaConSusCabeceras() throws Exception {
        List<Linea> lineas = lineasDe(generar("5.00"));

        Linea cantidadCab = buscar(lineas, "Cant.");
        Linea precioCab = buscar(lineas, "P. unit.");
        Linea descuentoCab = buscar(lineas, "Dscto.");
        Linea importeCab = buscar(lineas, "Importe");
        Linea cantidad = primero(lineas, "2");
        Linea precio = primero(lineas, "59.90");
        Linea descuento = primero(lineas, "5.00");
        Linea importe = primero(lineas, "119.80");

        assertThat(precio.derecha()).isCloseTo(precioCab.derecha(), within(3f));
        assertThat(descuento.derecha()).isCloseTo(descuentoCab.derecha(), within(3f));
        assertThat(importe.derecha()).isCloseTo(importeCab.derecha(), within(3f));
        assertThat(centro(cantidad)).isCloseTo(centro(cantidadCab), within(3f));
    }

    @Test
    @DisplayName("CA-5-45 la fila de descuentos solo aparece cuando hay descuento")
    void descuentosSoloSiHay() throws Exception {
        assertThat(lineasDe(generar("0.00")).stream().anyMatch(l -> l.texto().contains("Descuentos"))).isFalse();
        assertThat(lineasDe(generar("12.00")).stream().anyMatch(l -> l.texto().contains("Descuentos"))).isTrue();
    }

    private static Linea primero(List<Linea> lineas, String texto) {
        return lineas.stream().filter(l -> l.texto().equals(texto)).findFirst()
                .orElseThrow(() -> new AssertionError("No hay texto \"" + texto + "\" en " + lineas));
    }

    private static float centro(Linea l) {
        return (l.izquierda() + l.derecha()) / 2;
    }

    private static Linea buscar(List<Linea> lineas, String parte) {
        return lineas.stream().filter(l -> l.texto().contains(parte)).reduce((a, b) -> b)
                .orElseThrow(() -> new AssertionError("No hay texto con \"" + parte + "\" en " + lineas));
    }

    private static List<Linea> lineasDe(byte[] pdf) throws IOException {
        List<Linea> lineas = new ArrayList<>();
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFTextStripper extractor = new PDFTextStripper() {
                @Override
                protected void writeString(String texto, List<TextPosition> posiciones) {
                    if (texto.isBlank()) {
                        return;
                    }
                    float izq = Float.MAX_VALUE;
                    float der = 0;
                    float letra = 0;
                    for (TextPosition p : posiciones) {
                        izq = Math.min(izq, p.getXDirAdj());
                        der = Math.max(der, p.getXDirAdj() + p.getWidthDirAdj());
                        letra = Math.max(letra, p.getFontSizeInPt());
                    }
                    lineas.add(new Linea(texto.trim(), izq, der, letra));
                }
            };
            extractor.setSortByPosition(true);
            extractor.getText(documento);
        }
        return lineas;
    }

    private static byte[] generar(String descuentos) {
        ClassLoaderTemplateResolver resolvedor = new ClassLoaderTemplateResolver();
        resolvedor.setPrefix("templates/");
        resolvedor.setSuffix(".html");
        resolvedor.setCharacterEncoding("UTF-8");
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolvedor);
        Map<String, Object> v = new HashMap<>();
        v.put("numero", "B001-00000046");
        v.put("fecha", "21/10/2026 10:15");
        v.put("emisor", Map.of("razonSocial", "Moda Activa S.A.C.", "ruc", "20123456789",
                "direccionFiscal", "Jr. Gamarra 123, La Victoria"));
        v.put("logo", null);
        v.put("cliente", Map.of("nombre", "Ana Quispe Rojas", "dni", "12345678"));
        v.put("codigoPedido", "FS-000124");
        v.put("medioPago", "Visa ****1111");
        v.put("lineas", List.of(new LineaBoleta("Polo Dry-Fit", "Talla M · Negro", 2, "59.90", "5.00", "119.80")));
        v.put("subtotal", "119.80");
        v.put("descuentos", descuentos);
        v.put("envio", "10.00");
        v.put("total", "129.80");
        v.put("igv", "19.80");
        v.put("leyenda", "Documento simulado sin validez tributaria");
        return new BoletaPdf(motor).generar(v);
    }
}
