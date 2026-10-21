package com.fitstore.notification.plantillas;

import java.util.List;
import java.util.Map;

/**
 * Datos de ejemplo fijos por plantilla (SPEC-7 N3): realistas, con la clienta "Ana Quispe" y el pedido FS-000124 de
 * tres productos, para la vista previa y las pruebas de render. Trae las mismas claves que arma cada manejador.
 */
public final class EjemplosPlantilla {

    private static final String TIENDA = "http://localhost:4200";
    private static final String PEDIDO = "FS-000124";

    private EjemplosPlantilla() {
    }

    public static Map<String, Object> de(CatalogoPlantillas plantilla) {
        VariablesCorreo v = VariablesCorreo.nuevo().con("nombre", "Ana");
        return switch (plantilla) {
            case BIENVENIDA, PASSWORD_CAMBIADO -> v.construir();
            case RECUPERAR_PASSWORD -> v.boton(TIENDA + "/restablecer?token=ejemplo").construir();
            case INVITACION_STAFF -> v.boton("http://localhost:4300/restablecer?token=ejemplo").construir();
            case PEDIDO_CONFIRMADO -> v.con("codigoPedido", PEDIDO).con("total", "S/ 332.46")
                    .con("modalidad", "Envío por agencia")
                    .con(VariablesCorreo.ITEMS, List.of(
                            item("Legging Power", "M", "Negro", 2, "S/ 79.90", "S/ 159.80"),
                            item("Jogger Flex", "L", "Negro", 1, "S/ 129.90", "S/ 129.90"),
                            item("Top Run", "S", "Rosado", 1, "S/ 29.90", "S/ 29.90")))
                    .con("subtotal", "S/ 319.60").con("descuento", "S/ 0.00").con("costoEnvio", "S/ 12.86")
                    .fila("Agencia", "Shalom").boton(TIENDA + "/cuenta/pedidos/" + PEDIDO).construir();
            case PEDIDO_CANCELADO -> v.con("codigoPedido", PEDIDO).con("motivo", "Pedido pagado fuera de plazo")
                    .fila("Motivo", "Pedido pagado fuera de plazo").boton(TIENDA + "/cuenta/pedidos/" + PEDIDO).construir();
            case REEMBOLSO_REALIZADO -> v.con("codigoPedido", PEDIDO).con("monto", "S/ 79.90")
                    .con("motivo", "Postventa: producto defectuoso")
                    .fila("Tipo", "Reembolso parcial de tu pedido").fila("Monto", "S/ 79.90")
                    .fila("Motivo", "Postventa: producto defectuoso").boton(TIENDA + "/cuenta/pedidos/" + PEDIDO).construir();
            case EN_CAMINO -> v.con("codigoPedido", PEDIDO).fila("Agencia", "Shalom")
                    .fila("Código de seguimiento", "SH-2026-48121")
                    .filaConEnlace("Rastrea tu envío", "https://rastreo.shalom.test/SH-2026-48121",
                            "https://rastreo.shalom.test/SH-2026-48121")
                    .boton(TIENDA + "/cuenta/pedidos/" + PEDIDO).construir();
            case ENTREGA_FALLIDA -> v.con("codigoPedido", PEDIDO).con("observacion", "Nadie en casa")
                    .fila("Observación", "Nadie en casa").construir();
            case LISTO_PARA_RECOGER -> v.con("codigoPedido", PEDIDO).fila("Agencia", "Shalom")
                    .fila("Clave de recojo", "K-4821").boton(TIENDA + "/cuenta/pedidos/" + PEDIDO).construir();
            case ENTREGADO -> v.con("codigoPedido", PEDIDO).construir();
            case RECLAMO_COPIA -> v.con("numeroReclamo", "RC-2026-000007").con("fechaLimite", "jueves 5 de noviembre")
                    .fila("Número", "RC-2026-000007").fila("Tipo", "Reclamo")
                    .fila("Detalle", "El producto llegó con un hilo suelto")
                    .boton(TIENDA + "/libro-de-reclamaciones").construir();
            case RECLAMO_RESPUESTA -> v.con("numeroReclamo", "RC-2026-000007")
                    .fila("Respuesta", "Cambiaremos tu producto sin costo.").boton(TIENDA + "/libro-de-reclamaciones").construir();
            case PEDIDO_DEVUELTO -> v.con("codigoPedido", PEDIDO).con("motivo", "La agencia no pudo entregarlo")
                    .fila("Motivo", "La agencia no pudo entregarlo").construir();
            case ALERTA_STOCK_BAJO -> VariablesCorreo.nuevo().con("sku", "LEG01-NEG-M").con("producto", "Legging Power")
                    .con("talla", "M").con("color", "Negro").con("disponible", "2").con("minimo", "5").construir();
            case ALERTA_SIN_STOCK -> VariablesCorreo.nuevo().con("codigoPedido", PEDIDO)
                    .fila("LEG01-NEG-M", "Pidieron 2 y hay 0").construir();
            case ALERTA_REEMBOLSO_FALLIDO -> VariablesCorreo.nuevo().con("codigoPedido", PEDIDO).con("monto", "S/ 332.46")
                    .con("ultimoError", "Timeout del proveedor de pagos").construir();
            case ALERTA_RECLAMO_NUEVO -> VariablesCorreo.nuevo().con("numeroReclamo", "RC-2026-000007")
                    .con("tipo", "Reclamo").con("fechaLimite", "jueves 5 de noviembre").construir();
        };
    }

    /** Fila de la tabla de productos del correo de confirmación. */
    public static Map<String, Object> item(String producto, String talla, String color, int cantidad, String precio,
            String total) {
        return Map.of("producto", producto, "talla", talla, "color", color, "cantidad", cantidad,
                "precioUnitario", precio, "total", total);
    }
}
