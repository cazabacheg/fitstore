package com.fitstore.order.pedidos.service.dominio;

import static com.fitstore.order.pedidos.model.EstadoPedido.CANCELADO;
import static com.fitstore.order.pedidos.model.EstadoPedido.DEVUELTO;
import static com.fitstore.order.pedidos.model.EstadoPedido.EN_PREPARACION;
import static com.fitstore.order.pedidos.model.EstadoPedido.ENTREGADO;
import static com.fitstore.order.pedidos.model.EstadoPedido.ENVIADO;
import static com.fitstore.order.pedidos.model.EstadoPedido.EXPIRADO;
import static com.fitstore.order.pedidos.model.EstadoPedido.LISTO_DESPACHO;
import static com.fitstore.order.pedidos.model.EstadoPedido.LISTO_RECOJO;
import static com.fitstore.order.pedidos.model.EstadoPedido.PAGADO;
import static com.fitstore.order.pedidos.model.EstadoPedido.PENDIENTE_PAGO;
import static com.fitstore.order.pedidos.model.Modalidad.AGENCIA;
import static com.fitstore.order.pedidos.model.Modalidad.DELIVERY_LOCAL;
import static com.fitstore.order.pedidos.model.Modalidad.RECOJO;
import static com.fitstore.order.pedidos.model.TipoActor.CLIENTE;
import static com.fitstore.order.pedidos.model.TipoActor.SISTEMA;
import static com.fitstore.order.pedidos.model.TipoActor.STAFF;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.TipoActor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Máquina de estados del pedido (micro 5 y 10.2): una tabla en memoria {@code (desde, hacia) → actores y modalidades}.
 * Es el único lugar donde viven las reglas; la API del backoffice solo dibuja las transiciones que esta tabla permite (O4).
 */
@Component
public class MaquinaEstadosPedido {

    /** Botón de avance del backoffice: estado destino, texto y qué pide la acción. */
    public record Transicion(EstadoPedido estado, String etiqueta, boolean requiereComentario, boolean requiereDni) {
    }

    private record Regla(EstadoPedido desde, EstadoPedido hacia, Set<TipoActor> actores, Set<Modalidad> modalidades,
            Transicion boton) {
    }

    private static final Set<Modalidad> TODAS = Set.of(DELIVERY_LOCAL, AGENCIA, RECOJO);
    private static final Set<Modalidad> CON_ENVIO = Set.of(DELIVERY_LOCAL, AGENCIA);
    private static final Set<Modalidad> SOLO_RECOJO = Set.of(RECOJO);

    private static final List<Regla> REGLAS = List.of(
            regla(PENDIENTE_PAGO, PAGADO, Set.of(SISTEMA), TODAS, null),
            regla(PENDIENTE_PAGO, EXPIRADO, Set.of(SISTEMA), TODAS, null),
            regla(PENDIENTE_PAGO, CANCELADO, Set.of(CLIENTE, STAFF), TODAS, null),
            regla(PAGADO, EN_PREPARACION, Set.of(STAFF), TODAS,
                    new Transicion(EN_PREPARACION, "Empezar a preparar", false, false)),
            regla(PAGADO, CANCELADO, Set.of(CLIENTE, STAFF), TODAS, null),
            regla(EN_PREPARACION, LISTO_DESPACHO, Set.of(STAFF), CON_ENVIO,
                    new Transicion(LISTO_DESPACHO, "Marcar listo para despacho", false, false)),
            regla(EN_PREPARACION, LISTO_RECOJO, Set.of(STAFF), SOLO_RECOJO,
                    new Transicion(LISTO_RECOJO, "Marcar listo para recoger", false, false)),
            regla(EN_PREPARACION, CANCELADO, Set.of(STAFF), TODAS, null),
            regla(LISTO_DESPACHO, ENVIADO, Set.of(SISTEMA), CON_ENVIO, null),
            regla(ENVIADO, ENTREGADO, Set.of(SISTEMA), CON_ENVIO, null),
            regla(ENVIADO, DEVUELTO, Set.of(SISTEMA), CON_ENVIO, null),
            regla(LISTO_RECOJO, ENTREGADO, Set.of(STAFF), SOLO_RECOJO,
                    new Transicion(ENTREGADO, "Marcar recogido", false, true)),
            regla(LISTO_RECOJO, DEVUELTO, Set.of(STAFF), SOLO_RECOJO,
                    new Transicion(DEVUELTO, "Marcar no recogido", true, false)));

    private static Regla regla(EstadoPedido desde, EstadoPedido hacia, Set<TipoActor> actores,
            Set<Modalidad> modalidades, Transicion boton) {
        return new Regla(desde, hacia, actores, modalidades, boton);
    }

    /** Lanza 409 {@code TRANSICION_INVALIDA} con el estado actual si el actor no puede hacer ese paso. */
    public void validar(EstadoPedido desde, EstadoPedido hacia, TipoActor actor, Modalidad modalidad) {
        boolean permitida = REGLAS.stream().anyMatch(r -> r.desde() == desde && r.hacia() == hacia
                && r.actores().contains(actor) && r.modalidades().contains(modalidad));
        if (!permitida) {
            throw new ConflictoNegocioException("TRANSICION_INVALIDA",
                    "No se puede pasar el pedido de " + desde + " a " + hacia,
                    Map.<String, Object>of("estadoActual", desde.name()));
        }
    }

    /** Cancelar: el cliente en PENDIENTE_PAGO y PAGADO; el staff hasta EN_PREPARACION. */
    public boolean puedeCancelar(EstadoPedido estado, TipoActor actor) {
        return REGLAS.stream().anyMatch(r -> r.desde() == estado && r.hacia() == CANCELADO && r.actores().contains(actor));
    }

    /** Los botones de avance que ve el staff para ese estado y modalidad (sin cancelar, que va aparte). */
    public List<Transicion> transicionesDeStaff(EstadoPedido estado, Modalidad modalidad) {
        return REGLAS.stream()
                .filter(r -> r.desde() == estado && r.boton() != null && r.actores().contains(STAFF)
                        && r.modalidades().contains(modalidad))
                .map(Regla::boton).toList();
    }
}
