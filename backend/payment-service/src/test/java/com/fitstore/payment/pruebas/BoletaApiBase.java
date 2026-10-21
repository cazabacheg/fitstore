package com.fitstore.payment.pruebas;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.Transaccion;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Base de las pruebas de boleta: Garage real en Testcontainers (bucket privado) y una intención ya cobrada. */
public abstract class BoletaApiBase extends PagoApiBase {

    @DynamicPropertySource
    static void garage(DynamicPropertyRegistry registro) {
        GaragePrueba.registrar(registro);
    }

    /** Intención PAGADA de S/ 129.80 (S/ 119.80 en productos y S/ 10.00 de envío) con su cobro aprobado. */
    protected IntencionPago intencionPagada(UUID clienteId) {
        IntencionPago i = intencion(clienteId, "129.80", Instant.now().plus(30, ChronoUnit.MINUTES));
        i.setItems(List.of(
                Map.of("sku", "POL-001-M-NEG", "producto", "Polo Dry-Fit", "talla", "M", "color", "Negro", "cantidad", 2,
                        "precioUnitario", 59.90, "descuentoPorcentaje", 0, "descuentoMonto", 0, "total", 119.80)));
        i.setCliente(Map.of("nombre", "Ana Quispe Rojas", "dni", "12345678", "email", "ana@correo.pe"));
        i.setSubtotal(new BigDecimal("119.80"));
        i.setDescuentoTotal(new BigDecimal("0.00"));
        i.setCostoEnvio(new BigDecimal("10.00"));
        i.setEstado(EstadoIntencion.PAGADA);
        i = intenciones.saveAndFlush(i);
        Transaccion t = Datos.transaccion(i.getId(), EstadoTransaccion.APROBADA, UUID.randomUUID().toString());
        t.setProveedor(Proveedor.MERCADO_PAGO);
        t.setMedio(Medio.TARJETA);
        t.setMonto(i.getMonto());
        t.setRespuesta(Map.of("descripcionMedio", "Visa ****1111"));
        transacciones.saveAndFlush(t);
        return i;
    }

    protected static String textoDelPdf(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento).replaceAll("\\s+", " ");
        }
    }

    protected String numeroDeBoleta(UUID intencionId) {
        return jdbc.queryForObject("SELECT numero FROM boleta WHERE intencion_id = ?", String.class, intencionId);
    }
}
