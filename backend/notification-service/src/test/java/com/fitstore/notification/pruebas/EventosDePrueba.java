package com.fitstore.notification.pruebas;

import com.fitstore.common.event.SobreEvento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/** Arma los eventos tal como los publica cada servicio (AsyncAPI), dentro del sobre común. */
public final class EventosDePrueba {

    public static final UUID PEDIDO = UUID.fromString("a0000000-0000-4000-8000-000000000124");
    public static final String CODIGO = "FS-000124";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private EventosDePrueba() {
    }

    public static SobreEvento sobre(String tipo, Map<String, Object> datos) {
        return new SobreEvento(UUID.randomUUID(), tipo, SobreEvento.VERSION_ACTUAL, Instant.now(), JSON.valueToTree(datos));
    }

    public static Map<String, Object> pedidoCreado(String modalidad) {
        return Map.ofEntries(
                Map.entry("pedidoId", PEDIDO.toString()),
                Map.entry("codigo", CODIGO),
                Map.entry("clienteId", "35a11d4f-054a-44a5-8ac4-cf0ed4020f8c"),
                Map.entry("destinatario", Map.of("nombres", "Ana", "apellidos", "Quispe Mamani", "dni", "70123499",
                        "telefono", "987000099", "email", "ana.quispe@fitstore.test")),
                Map.entry("modalidad", modalidad),
                Map.entry("items", List.of(
                        item("LEG01-NEG-M", "Legging Power", "M", "Negro", "79.90", 2, "159.80"),
                        item("JOG01-NEG-L", "Jogger Flex", "L", "Negro", "129.90", 1, "129.90"))),
                Map.entry("subtotal", new BigDecimal("289.70")),
                Map.entry("descuentoTotal", new BigDecimal("0.00")),
                Map.entry("costoEnvio", new BigDecimal("10.00")),
                Map.entry("total", new BigDecimal("299.70")),
                Map.entry("expiraEn", Instant.parse("2026-10-22T16:00:00Z").toString()));
    }

    private static Map<String, Object> item(String sku, String producto, String talla, String color, String precio,
            int cantidad, String total) {
        return Map.of("varianteId", 10002, "productoId", 1001, "sku", sku, "producto", producto, "talla", talla,
                "color", color, "precioUnitario", new BigDecimal(precio), "cantidad", cantidad,
                "descuentoPorcentaje", BigDecimal.ZERO, "descuentoMonto", BigDecimal.ZERO,
                "total", new BigDecimal(total));
    }

    public static Map<String, Object> persona(String nombre, String email) {
        return Map.of("nombre", nombre, "email", email);
    }

    /** {@code EntregaActualizada} con los campos que cambian entre estados; el resto va con valores de demo. */
    public static Map<String, Object> entrega(String modalidad, String estado, Map<String, Object> extra) {
        Map<String, Object> datos = new java.util.HashMap<>(Map.of(
                "entregaId", UUID.randomUUID().toString(),
                "pedidoId", PEDIDO.toString(),
                "codigoPedido", CODIGO,
                "modalidad", modalidad,
                "estado", estado,
                "intento", 1,
                "destinatario", persona("Ana Quispe", "ana.quispe@fitstore.test")));
        datos.putAll(extra);
        return datos;
    }
}
