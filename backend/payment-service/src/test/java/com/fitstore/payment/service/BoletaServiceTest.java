package com.fitstore.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import com.fitstore.payment.dto.ConfiguracionPublica;
import com.fitstore.payment.model.Boleta;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.BoletaApiBase;
import com.fitstore.payment.pruebas.GaragePrueba;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Generación de la boleta simulada en PDF, guardada en el bucket privado de Garage (HU-5-09: CA-5-26). */
class BoletaServiceTest extends BoletaApiBase {

    @Autowired BoletaService boletas;
    @Autowired FileStorage almacenamiento;

    private final UUID clienta = UUID.randomUUID();

    @Test
    @DisplayName("CA-5-26 la boleta trae emisor, cliente, ítems con talla y color, envío, total con IGV, medio de pago, código del pedido y la leyenda")
    void contenidoDelPdf() throws Exception {
        IntencionPago i = intencionPagada(clienta);

        Boleta boleta = boletas.emitir(i.getId());

        assertThat(boleta.getNumero()).matches("B001-\\d{8}");
        byte[] pdf = almacenamiento.leer(TipoBucket.PRIVADO, boleta.getClave());
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        String texto = textoDelPdf(pdf);
        assertThat(texto)
                .contains(boleta.getNumero())
                .contains("Moda Activa S.A.C.").contains("20123456789").contains("Jr. Gamarra 123, La Victoria")
                .contains("Ana Quispe Rojas").contains("12345678")
                .contains("Polo Dry-Fit").contains("Talla M").contains("Negro")
                .contains("119.80").contains("10.00").contains("129.80")
                .contains("IGV").contains("19.80")
                .contains("Visa ****1111").contains("Mercado Pago")
                .contains(i.getCodigoPedido())
                .contains("Documento simulado sin validez tributaria");
    }

    @Test
    @DisplayName("CA-5-26 la boleta se guarda en el bucket privado como boletas/{año}/{número}.pdf y publica BoletaEmitida con la ruta autenticada")
    void seGuardaYPublica() {
        IntencionPago i = intencionPagada(clienta);

        Boleta boleta = boletas.emitir(i.getId());

        assertThat(boleta.getClave()).matches("boletas/\\d{4}/B001-\\d{8}\\.pdf");
        assertThat(almacenamiento.existe(TipoBucket.PRIVADO, boleta.getClave())).isTrue();
        assertThat(eventosEnOutbox(i.getPedidoId(), "BoletaEmitida")).isEqualTo(1);
        String payload = jdbc.queryForObject("SELECT payload::text FROM outbox_evento WHERE tipo = 'BoletaEmitida' AND agregado_id = ?",
                String.class, i.getPedidoId().toString());
        assertThat(payload).contains(boleta.getNumero()).contains("/api/v1/pagos/" + i.getCodigoPedido() + "/boleta");
    }

    @Test
    @DisplayName("CA-5-26 el logo del emisor (de la configuración pública) se incluye en el PDF")
    void incluyeElLogo() throws Exception {
        BufferedImage imagen = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", png);
        String clave = "logos/" + UUID.randomUUID() + ".png";
        almacenamiento.guardar(TipoBucket.PUBLICO, clave, png.toByteArray(), "image/png");
        when(tienda.publica()).thenReturn(Optional.of(new ConfiguracionPublica(
                new ConfiguracionPublica.Identidad("Moda Activa", "Moda Activa S.A.C.", "20123456789",
                        "Jr. Gamarra 123, La Victoria", "/media/" + clave),
                new ConfiguracionPublica.Parametros(List.of("MERCADO_PAGO")))));
        IntencionPago i = intencionPagada(clienta);

        Boleta boleta = boletas.emitir(i.getId());

        try (PDDocument documento = Loader.loadPDF(almacenamiento.leer(TipoBucket.PRIVADO, boleta.getClave()))) {
            PDResources recursos = documento.getPage(0).getResources();
            long imagenes = 0;
            for (COSName nombre : recursos.getXObjectNames()) {
                if (recursos.isImageXObject(nombre)) {
                    imagenes++;
                }
            }
            assertThat(imagenes).isPositive();
        }
    }

    @Test
    @DisplayName("CA-5-26 emitir dos veces la boleta de una intención devuelve la misma sin consumir otro correlativo")
    void unaSolaPorIntencion() {
        IntencionPago i = intencionPagada(clienta);

        Boleta primera = boletas.emitir(i.getId());
        Boleta repetida = boletas.emitir(i.getId());

        assertThat(repetida.getNumero()).isEqualTo(primera.getNumero());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM boleta WHERE intencion_id = ?", Integer.class, i.getId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-26 el proceso de boletas emite las pendientes de las intenciones ya cobradas")
    void emitePendientes() {
        IntencionPago i = intencionPagada(clienta);

        int emitidas = boletas.emitirPendientes();

        assertThat(emitidas).isGreaterThanOrEqualTo(1);
        assertThat(numeroDeBoleta(i.getId())).matches("B001-\\d{8}");
    }

    @Test
    @DisplayName("CA-5-26 un logo que no es PNG ni JPG, o que no existe en el bucket, se omite y la boleta se emite igual")
    void logoNoUsable() throws Exception {
        for (String logo : List.of("/media/logos/logo.svg", "/media/logos/no-existe.png", "https://otro.dominio/logo.png")) {
            when(tienda.publica()).thenReturn(Optional.of(new ConfiguracionPublica(
                    new ConfiguracionPublica.Identidad("Moda Activa", "Moda Activa S.A.C.", "20123456789",
                            "Jr. Gamarra 123, La Victoria", logo),
                    new ConfiguracionPublica.Parametros(List.of("MERCADO_PAGO")))));
            IntencionPago i = intencionPagada(clienta);

            Boleta boleta = boletas.emitir(i.getId());

            assertThat(textoDelPdf(almacenamiento.leer(TipoBucket.PRIVADO, boleta.getClave()))).contains("Moda Activa S.A.C.");
        }
    }

    @Test
    @DisplayName("CA-5-26 el medio de pago del pago de prueba, y los ítems sin talla ni color, se muestran bien")
    void medioSimuladoEItemsSimples() throws Exception {
        IntencionPago i = intencionPagada(clienta);
        i.setItems(List.of(java.util.Map.of("producto", "Gorra", "cantidad", 1, "precioUnitario", 129.80, "total", 129.80)));
        intenciones.saveAndFlush(i);
        jdbc.update("UPDATE transaccion SET proveedor = 'SIMULADO', respuesta = NULL WHERE intencion_id = ?", i.getId());

        Boleta boleta = boletas.emitir(i.getId());

        String texto = textoDelPdf(almacenamiento.leer(TipoBucket.PRIVADO, boleta.getClave()));
        assertThat(texto).contains("Pago de prueba").contains("Gorra");
    }

    @Test
    @DisplayName("CA-5-26 una boleta sin cobro aprobado muestra un guion como medio de pago")
    void sinCobroAprobado() throws Exception {
        IntencionPago i = intencionPagada(clienta);
        jdbc.update("UPDATE transaccion SET estado = 'RECHAZADA' WHERE intencion_id = ?", i.getId());

        Boleta boleta = boletas.emitir(i.getId());

        assertThat(textoDelPdf(almacenamiento.leer(TipoBucket.PRIVADO, boleta.getClave()))).contains("Medio de pago -");
    }
}
