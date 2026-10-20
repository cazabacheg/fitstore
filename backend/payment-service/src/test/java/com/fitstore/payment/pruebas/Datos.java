package com.fitstore.payment.pruebas;

import com.fitstore.payment.model.Boleta;
import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.EstadoReembolso;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.MotivoReembolso;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.Reembolso;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.model.WebhookRecibido;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Fábricas de entidades válidas (sin guardar) para las pruebas de repositorio. */
public final class Datos {

    private Datos() {
    }

    public static IntencionPago intencion() {
        IntencionPago i = new IntencionPago();
        i.setPedidoId(UUID.randomUUID());
        i.setCodigoPedido("FS-%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000)));
        i.setClienteId(UUID.randomUUID());
        i.setMonto(new BigDecimal("119.80"));
        i.setEstado(EstadoIntencion.PENDIENTE);
        i.setExpiraEn(Instant.now().plus(30, ChronoUnit.MINUTES));
        i.setItems(List.of(Map.of("sku", "POL-001-M-NEG", "cantidad", 2, "total", 119.80)));
        i.setCliente(Map.of("nombre", "Ana Pérez", "dni", "12345678", "email", "ana@correo.pe"));
        i.setSubtotal(new BigDecimal("119.80"));
        i.setDescuentoTotal(new BigDecimal("0.00"));
        i.setCostoEnvio(new BigDecimal("0.00"));
        return i;
    }

    public static Transaccion transaccion(UUID intencionId, EstadoTransaccion estado, String idempotencyKey) {
        Transaccion t = new Transaccion();
        t.setIntencionId(intencionId);
        t.setProveedor(Proveedor.SIMULADO);
        t.setMedio(Medio.TARJETA);
        t.setEstado(estado);
        t.setMonto(new BigDecimal("119.80"));
        t.setIdempotencyKey(idempotencyKey);
        return t;
    }

    public static Reembolso reembolso(UUID intencionId, UUID transaccionId, MotivoReembolso motivo) {
        Reembolso r = new Reembolso();
        r.setIntencionId(intencionId);
        r.setTransaccionId(transaccionId);
        r.setMonto(new BigDecimal("50.00"));
        r.setMotivo(motivo);
        r.setEstado(EstadoReembolso.PENDIENTE);
        return r;
    }

    public static Boleta boleta(UUID intencionId, int correlativo) {
        Boleta b = new Boleta();
        b.setIntencionId(intencionId);
        b.setSerie("B001");
        b.setCorrelativo(correlativo);
        b.setNumero("B001-%08d".formatted(correlativo));
        b.setClave("boletas/2026/B001-%08d.pdf".formatted(correlativo));
        b.setEmitidaEn(Instant.now());
        return b;
    }

    public static WebhookRecibido webhook(Proveedor proveedor, String eventoId) {
        WebhookRecibido w = new WebhookRecibido();
        w.setProveedor(proveedor);
        w.setEventoIdProveedor(eventoId);
        w.setPayload(Map.of("type", "payment.created", "data", Map.of("id", eventoId)));
        return w;
    }
}
