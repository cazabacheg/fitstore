package com.fitstore.payment.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Convierte la plantilla Thymeleaf {@code boleta.html} (P-5-06) en un PDF A4. */
@Component
public class BoletaPdf {

    private final TemplateEngine plantillas;

    public BoletaPdf(TemplateEngine plantillas) {
        this.plantillas = plantillas;
    }

    public byte[] generar(Map<String, Object> variables) {
        Context contexto = new Context();
        contexto.setVariables(variables);
        String html = plantillas.process("boleta", contexto);
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PdfRendererBuilder constructor = new PdfRendererBuilder();
            constructor.useFastMode();
            constructor.withHtmlContent(html, null);
            constructor.toStream(salida);
            constructor.run();
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la boleta", e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudo generar el PDF de la boleta", e);
        }
    }
}
