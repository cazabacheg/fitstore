package com.fitstore.order.pedidos;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.common.api.FitstoreException;
import com.fitstore.order.pedidos.model.EstadoPedido;
import com.fitstore.order.pedidos.model.Modalidad;
import com.fitstore.order.pedidos.model.TipoActor;
import com.fitstore.order.pedidos.service.dominio.MaquinaEstadosPedido;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tabla de todas las transiciones permitidas y prohibidas (micro 5), con sus actores y modalidades. */
class MaquinaEstadosPedidoTest {

    private final MaquinaEstadosPedido maquina = new MaquinaEstadosPedido();

    /** Las transiciones permitidas: desde, hacia, actor, modalidades en que aplica. */
    private static Stream<Arguments> permitidas() {
        List<Modalidad> todas = List.of(DELIVERY_LOCAL, AGENCIA, RECOJO);
        List<Modalidad> envio = List.of(DELIVERY_LOCAL, AGENCIA);
        return Stream.of(
                Arguments.of(PENDIENTE_PAGO, PAGADO, SISTEMA, todas),
                Arguments.of(PENDIENTE_PAGO, EXPIRADO, SISTEMA, todas),
                Arguments.of(PENDIENTE_PAGO, CANCELADO, CLIENTE, todas),
                Arguments.of(PENDIENTE_PAGO, CANCELADO, STAFF, todas),
                Arguments.of(PAGADO, EN_PREPARACION, STAFF, todas),
                Arguments.of(PAGADO, CANCELADO, CLIENTE, todas),
                Arguments.of(PAGADO, CANCELADO, STAFF, todas),
                Arguments.of(EN_PREPARACION, LISTO_DESPACHO, STAFF, envio),
                Arguments.of(EN_PREPARACION, LISTO_RECOJO, STAFF, List.of(RECOJO)),
                Arguments.of(EN_PREPARACION, CANCELADO, STAFF, todas),
                Arguments.of(LISTO_DESPACHO, ENVIADO, SISTEMA, envio),
                Arguments.of(ENVIADO, ENTREGADO, SISTEMA, envio),
                Arguments.of(ENVIADO, DEVUELTO, SISTEMA, envio),
                Arguments.of(LISTO_RECOJO, ENTREGADO, STAFF, List.of(RECOJO)),
                Arguments.of(LISTO_RECOJO, DEVUELTO, STAFF, List.of(RECOJO)));
    }

    @ParameterizedTest(name = "{0} → {1} por {2}")
    @MethodSource("permitidas")
    @DisplayName("CA-4-47 las transiciones de la tabla se permiten solo a su actor y en sus modalidades")
    void permitidasSoloParaSuActor(EstadoPedido desde, EstadoPedido hacia, TipoActor actor, List<Modalidad> modalidades) {
        for (Modalidad modalidad : Modalidad.values()) {
            if (modalidades.contains(modalidad)) {
                assertThatCode(() -> maquina.validar(desde, hacia, actor, modalidad)).doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> maquina.validar(desde, hacia, actor, modalidad))
                        .isInstanceOf(FitstoreException.class);
            }
        }
        for (TipoActor otro : TipoActor.values()) {
            if (otro != actor && !(esMismaTransicionDeOtroActor(desde, hacia, otro))) {
                assertThatThrownBy(() -> maquina.validar(desde, hacia, otro, modalidades.get(0)))
                        .isInstanceOf(FitstoreException.class);
            }
        }
    }

    private static boolean esMismaTransicionDeOtroActor(EstadoPedido desde, EstadoPedido hacia, TipoActor actor) {
        return permitidas().anyMatch(a -> a.get()[0] == desde && a.get()[1] == hacia && a.get()[2] == actor);
    }

    @Test
    @DisplayName("CA-4-47 cualquier par de estados fuera de la tabla se rechaza con 409 y el estado actual")
    void lasDemasSeRechazan() {
        long permitidasTotal = permitidas().map(a -> a.get()).mapToLong(a -> ((List<?>) a[3]).size()).sum();
        long aceptadas = 0;
        for (EstadoPedido desde : EstadoPedido.values()) {
            for (EstadoPedido hacia : EstadoPedido.values()) {
                for (TipoActor actor : TipoActor.values()) {
                    for (Modalidad modalidad : Modalidad.values()) {
                        try {
                            maquina.validar(desde, hacia, actor, modalidad);
                            aceptadas++;
                        } catch (FitstoreException e) {
                            assertThat(e.estado().value()).isEqualTo(409);
                            assertThat(e.codigo()).isEqualTo("TRANSICION_INVALIDA");
                            assertThat(e.detalles()).containsEntry("estadoActual", desde.name());
                        }
                    }
                }
            }
        }
        // Cada fila de la tabla vale una vez por modalidad en que aplica; ninguna otra combinación pasa
        assertThat(aceptadas).isEqualTo(permitidasTotal);
    }

    @Test
    @DisplayName("CA-4-47 los estados finales (incluido DEVUELTO) no tienen salida")
    void estadosFinales() {
        for (EstadoPedido final_ : List.of(ENTREGADO, CANCELADO, EXPIRADO, DEVUELTO)) {
            for (EstadoPedido hacia : EstadoPedido.values()) {
                assertThatThrownBy(() -> maquina.validar(final_, hacia, STAFF, RECOJO))
                        .isInstanceOf(FitstoreException.class);
            }
        }
    }

    @Test
    @DisplayName("CA-4-47 LISTO_RECOJO en un pedido con agencia y PAGADO → ENVIADO se rechazan")
    void ejemplosDeLaSpec() {
        assertThatThrownBy(() -> maquina.validar(EN_PREPARACION, LISTO_RECOJO, STAFF, AGENCIA))
                .isInstanceOf(FitstoreException.class);
        assertThatThrownBy(() -> maquina.validar(PAGADO, ENVIADO, STAFF, DELIVERY_LOCAL))
                .isInstanceOf(FitstoreException.class);
    }

    @Test
    @DisplayName("CA-4-38 el cliente cancela en PENDIENTE_PAGO y PAGADO; CA-4-39 después no")
    void clienteCancelaHastaPagado() {
        assertThat(maquina.puedeCancelar(PENDIENTE_PAGO, CLIENTE)).isTrue();
        assertThat(maquina.puedeCancelar(PAGADO, CLIENTE)).isTrue();
        for (EstadoPedido estado : List.of(EN_PREPARACION, LISTO_DESPACHO, LISTO_RECOJO, ENVIADO, ENTREGADO,
                CANCELADO, EXPIRADO, DEVUELTO)) {
            assertThat(maquina.puedeCancelar(estado, CLIENTE)).as(estado.name()).isFalse();
        }
    }

    @Test
    @DisplayName("CA-4-49 el staff cancela hasta EN_PREPARACION; CA-4-50 desde LISTO_DESPACHO no")
    void staffCancelaHastaEnPreparacion() {
        for (EstadoPedido estado : List.of(PENDIENTE_PAGO, PAGADO, EN_PREPARACION)) {
            assertThat(maquina.puedeCancelar(estado, STAFF)).as(estado.name()).isTrue();
        }
        for (EstadoPedido estado : List.of(LISTO_DESPACHO, LISTO_RECOJO, ENVIADO, ENTREGADO, CANCELADO, EXPIRADO,
                DEVUELTO)) {
            assertThat(maquina.puedeCancelar(estado, STAFF)).as(estado.name()).isFalse();
        }
    }

    @Test
    @DisplayName("CA-4-43 el staff solo ve las transiciones de su estado y modalidad (O4)")
    void transicionesParaElBackoffice() {
        assertThat(maquina.transicionesDeStaff(PAGADO, DELIVERY_LOCAL)).extracting("estado")
                .containsExactly(EN_PREPARACION);
        assertThat(maquina.transicionesDeStaff(EN_PREPARACION, AGENCIA)).extracting("estado")
                .containsExactly(LISTO_DESPACHO);
        assertThat(maquina.transicionesDeStaff(EN_PREPARACION, RECOJO)).extracting("estado")
                .containsExactly(LISTO_RECOJO);
        var recojo = maquina.transicionesDeStaff(LISTO_RECOJO, RECOJO);
        assertThat(recojo).extracting("estado").containsExactly(ENTREGADO, DEVUELTO);
        assertThat(recojo.get(0).requiereDni()).isTrue();
        assertThat(recojo.get(1).requiereComentario()).isTrue();
        assertThat(maquina.transicionesDeStaff(ENVIADO, AGENCIA)).isEmpty();
        assertThat(maquina.transicionesDeStaff(PENDIENTE_PAGO, RECOJO)).isEmpty();
        assertThat(maquina.transicionesDeStaff(DEVUELTO, RECOJO)).isEmpty();
    }
}
