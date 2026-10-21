package com.fitstore.notification.plantillas;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Las 18 plantillas (micro 4.1 y 10.2): 14 correos al cliente y 4 alertas a la tienda. El código es el de la tabla
 * {@code plantilla}. Cada una declara las variables que sus textos pueden usar con {@code {{nombre}}}.
 */
public enum CatalogoPlantillas {

    BIENVENIDA("Bienvenida", Para.CLIENTE, "se registra un cliente", false, "generico", "nombre"),
    RECUPERAR_PASSWORD("Recupera tu contraseña", Para.CLIENTE, "se solicita recuperar la contraseña", true, "generico", "nombre"),
    INVITACION_STAFF("Te invitaron al backoffice", Para.CLIENTE, "se invita a una persona del equipo", true, "generico", "nombre"),
    PASSWORD_CAMBIADO("Tu contraseña cambió", Para.CLIENTE, "se cambia una contraseña", false, "generico", "nombre"),
    PEDIDO_CONFIRMADO("¡Pedido confirmado!", Para.CLIENTE, "se emite la boleta (con adjunto)", true, "pedido-confirmado",
            "nombre", "codigoPedido", "total", "modalidad"),
    PEDIDO_CANCELADO("Pedido cancelado", Para.CLIENTE, "se cancela un pedido", false, "generico", "nombre", "codigoPedido", "motivo"),
    REEMBOLSO_REALIZADO("Reembolso realizado", Para.CLIENTE, "se devuelve el dinero", false, "generico",
            "nombre", "codigoPedido", "monto", "motivo"),
    EN_CAMINO("Tu pedido va en camino", Para.CLIENTE, "el pedido sale en ruta o por agencia", false, "generico",
            "nombre", "codigoPedido"),
    ENTREGA_FALLIDA("No pudimos entregar tu pedido", Para.CLIENTE, "la entrega falla", false, "generico",
            "nombre", "codigoPedido", "observacion"),
    LISTO_PARA_RECOGER("Tu pedido está listo para recoger", Para.CLIENTE,
            "el pedido llega a la agencia o queda listo en la tienda", false, "generico", "nombre", "codigoPedido"),
    ENTREGADO("¡Pedido entregado!", Para.CLIENTE, "se entrega o se recoge el pedido", false, "generico", "nombre", "codigoPedido"),
    RECLAMO_COPIA("Copia de tu reclamo", Para.CLIENTE, "se registra un reclamo", true, "generico",
            "nombre", "numeroReclamo", "fechaLimite"),
    RECLAMO_RESPUESTA("Respuesta a tu reclamo", Para.CLIENTE, "se responde un reclamo", true, "generico",
            "nombre", "numeroReclamo"),
    PEDIDO_DEVUELTO("Tu pedido regresó a la tienda", Para.CLIENTE, "el pedido vuelve a la tienda", false, "generico",
            "nombre", "codigoPedido", "motivo"),
    ALERTA_STOCK_BAJO("Stock bajo", Para.TIENDA, "una variante baja de su stock mínimo", false, "generico",
            "sku", "producto", "talla", "color", "disponible", "minimo"),
    ALERTA_SIN_STOCK("Pedido pagado sin stock", Para.TIENDA, "un pedido pagado no encuentra stock", false, "generico",
            "codigoPedido"),
    ALERTA_REEMBOLSO_FALLIDO("Reembolso fallido", Para.TIENDA, "falla un reembolso", false, "generico",
            "codigoPedido", "monto", "ultimoError"),
    ALERTA_RECLAMO_NUEVO("Nuevo reclamo", Para.TIENDA, "se registra un reclamo", false, "generico",
            "numeroReclamo", "tipo", "fechaLimite");

    /** A quién va el correo. */
    public enum Para {
        CLIENTE, TIENDA
    }

    private final String nombre;
    private final Para para;
    private final String evento;
    private final boolean obligatoria;
    private final String vista;
    private final Set<String> variables;

    CatalogoPlantillas(String nombre, Para para, String evento, boolean obligatoria, String vista, String... variables) {
        this.nombre = nombre;
        this.para = para;
        this.evento = evento;
        this.obligatoria = obligatoria;
        this.vista = vista;
        Set<String> todas = new LinkedHashSet<>(Arrays.asList(variables));
        todas.add("nombreTienda");
        this.variables = Set.copyOf(todas);
    }

    public String nombre() {
        return nombre;
    }

    public Para para() {
        return para;
    }

    public String evento() {
        return evento;
    }

    /** Las obligatorias no se pueden desactivar (recuperación, invitación, pedido confirmado y reclamos). */
    public boolean obligatoria() {
        return obligatoria;
    }

    /** Vista Thymeleaf de {@code templates/email} con el bloque propio del correo. */
    public String vista() {
        return vista;
    }

    /** Variables que los textos pueden usar; {@code nombreTienda} está en todas. */
    public Set<String> variablesPermitidas() {
        return variables;
    }
}
