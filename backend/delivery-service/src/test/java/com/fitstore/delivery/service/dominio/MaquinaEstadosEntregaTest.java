package com.fitstore.delivery.service.dominio;

import static com.fitstore.delivery.model.EstadoEntrega.ASIGNADA;
import static com.fitstore.delivery.model.EstadoEntrega.DEVUELTA;
import static com.fitstore.delivery.model.EstadoEntrega.DISPONIBLE_EN_AGENCIA;
import static com.fitstore.delivery.model.EstadoEntrega.ENTREGADA;
import static com.fitstore.delivery.model.EstadoEntrega.ENVIADO_AGENCIA;
import static com.fitstore.delivery.model.EstadoEntrega.EN_RUTA;
import static com.fitstore.delivery.model.EstadoEntrega.FALLIDA;
import static com.fitstore.delivery.model.EstadoEntrega.PENDIENTE_ASIGNACION;
import static com.fitstore.delivery.model.EstadoEntrega.PENDIENTE_ENVIO;
import static com.fitstore.delivery.model.Modalidad.AGENCIA;
import static com.fitstore.delivery.model.Modalidad.DELIVERY_LOCAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.model.AccionEntrega;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.service.dominio.MaquinaEstadosEntrega.DatosAvance;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Núcleo: las dos tablas de transiciones (local y agencia) y los datos obligatorios de cada avance (CA-6-20 a CA-6-23). */
class MaquinaEstadosEntregaTest {

    private static final DatosAvance TODO = new DatosAvance("No había nadie", "SH-12345");

    static List<Arguments> permitidas() {
        return List.of(
                Arguments.of(DELIVERY_LOCAL, PENDIENTE_ASIGNACION, ASIGNADA),
                Arguments.of(DELIVERY_LOCAL, ASIGNADA, ASIGNADA),
                Arguments.of(DELIVERY_LOCAL, ASIGNADA, EN_RUTA),
                Arguments.of(DELIVERY_LOCAL, EN_RUTA, ENTREGADA),
                Arguments.of(DELIVERY_LOCAL, EN_RUTA, FALLIDA),
                Arguments.of(DELIVERY_LOCAL, FALLIDA, ASIGNADA),
                Arguments.of(AGENCIA, PENDIENTE_ENVIO, ENVIADO_AGENCIA),
                Arguments.of(AGENCIA, ENVIADO_AGENCIA, DISPONIBLE_EN_AGENCIA),
                Arguments.of(AGENCIA, DISPONIBLE_EN_AGENCIA, ENTREGADA));
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("permitidas")
    @DisplayName("CA-6-20 CA-6-21 CA-6-27 las transiciones del flujo se permiten")
    void permiteElFlujo(Modalidad modalidad, EstadoEntrega desde, EstadoEntrega hacia) {
        assertThat(MaquinaEstadosEntrega.permitida(modalidad, desde, hacia)).isTrue();
        assertThatCode(() -> MaquinaEstadosEntrega.validar(modalidad, desde, hacia, TODO)).doesNotThrowAnyException();
    }

    /** Todas las parejas (modalidad, desde, hacia) que no están en la tabla de permitidas. */
    static List<Arguments> prohibidas() {
        List<Arguments> todas = new java.util.ArrayList<>();
        List<Arguments> ok = permitidas();
        for (Modalidad modalidad : Modalidad.values()) {
            for (EstadoEntrega desde : EstadoEntrega.values()) {
                for (EstadoEntrega hacia : EstadoEntrega.values()) {
                    boolean permitida = ok.stream().anyMatch(a -> a.get()[0] == modalidad && a.get()[1] == desde
                            && a.get()[2] == hacia);
                    if (!permitida) {
                        todas.add(Arguments.of(modalidad, desde, hacia));
                    }
                }
            }
        }
        return todas;
    }

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @MethodSource("prohibidas")
    @DisplayName("CA-6-23 cualquier otra transición se rechaza con 409 y el estado actual")
    void rechazaElResto(Modalidad modalidad, EstadoEntrega desde, EstadoEntrega hacia) {
        assertThat(MaquinaEstadosEntrega.permitida(modalidad, desde, hacia)).isFalse();
        assertThatThrownBy(() -> MaquinaEstadosEntrega.validar(modalidad, desde, hacia, TODO))
                .isInstanceOfSatisfying(ConflictoNegocioException.class, e -> {
                    assertThat(e.estado().value()).isEqualTo(409);
                    assertThat(e.detalles()).containsEntry("estadoActual", desde.name());
                });
    }

    @Test
    @DisplayName("CA-6-23 la devolución a tienda no está en la máquina: es extensión de alcance")
    void sinDevuelta() {
        assertThat(MaquinaEstadosEntrega.permitida(DELIVERY_LOCAL, FALLIDA, DEVUELTA)).isFalse();
        assertThat(MaquinaEstadosEntrega.permitida(AGENCIA, DISPONIBLE_EN_AGENCIA, DEVUELTA)).isFalse();
        assertThat(MaquinaEstadosEntrega.accionesPermitidas(DELIVERY_LOCAL, FALLIDA)).doesNotContain(AccionEntrega.DEVUELTA);
    }

    @Test
    @DisplayName("CA-6-22 marcar fallida exige la observación")
    void fallidaExigeObservacion() {
        for (String vacia : new String[] {null, "", "   "}) {
            assertThatThrownBy(() -> MaquinaEstadosEntrega.validar(DELIVERY_LOCAL, EN_RUTA, FALLIDA,
                    new DatosAvance(vacia, null)))
                    .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                            e -> assertThat(e.errores().get(0).campo()).isEqualTo("observacion"));
        }
    }

    @Test
    @DisplayName("CA-6-26 registrar el envío por agencia exige el código de seguimiento")
    void envioExigeCodigo() {
        for (String vacio : new String[] {null, "", "  "}) {
            assertThatThrownBy(() -> MaquinaEstadosEntrega.validar(AGENCIA, PENDIENTE_ENVIO, ENVIADO_AGENCIA,
                    new DatosAvance(null, vacio)))
                    .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                            e -> assertThat(e.errores().get(0).campo()).isEqualTo("codigoSeguimiento"));
        }
    }

    @Test
    @DisplayName("CA-6-23 el estado se comprueba antes que los datos del avance")
    void primeroElEstado() {
        assertThatThrownBy(() -> MaquinaEstadosEntrega.validar(DELIVERY_LOCAL, ENTREGADA, FALLIDA, new DatosAvance(null, null)))
                .isInstanceOf(ConflictoNegocioException.class);
    }

    @Test
    @DisplayName("CA-6-31 cada estado ofrece sus acciones y el siguiente paso de la fila (E1)")
    void accionesYSiguientePaso() {
        assertThat(MaquinaEstadosEntrega.accionesPermitidas(DELIVERY_LOCAL, PENDIENTE_ASIGNACION))
                .containsExactly(AccionEntrega.ASIGNAR);
        assertThat(MaquinaEstadosEntrega.accionesPermitidas(DELIVERY_LOCAL, ASIGNADA))
                .containsExactly(AccionEntrega.REPROGRAMAR, AccionEntrega.EN_RUTA);
        assertThat(MaquinaEstadosEntrega.accionesPermitidas(DELIVERY_LOCAL, EN_RUTA))
                .containsExactly(AccionEntrega.ENTREGADA, AccionEntrega.FALLIDA);
        assertThat(MaquinaEstadosEntrega.accionesPermitidas(DELIVERY_LOCAL, FALLIDA)).containsExactly(AccionEntrega.REPROGRAMAR);
        assertThat(MaquinaEstadosEntrega.accionesPermitidas(AGENCIA, PENDIENTE_ENVIO))
                .containsExactly(AccionEntrega.ENVIADA_AGENCIA);
        assertThat(MaquinaEstadosEntrega.accionesPermitidas(AGENCIA, ENVIADO_AGENCIA))
                .containsExactly(AccionEntrega.DISPONIBLE_AGENCIA);
        assertThat(MaquinaEstadosEntrega.accionesPermitidas(AGENCIA, DISPONIBLE_EN_AGENCIA))
                .containsExactly(AccionEntrega.ENTREGADA);
        assertThat(MaquinaEstadosEntrega.accionesPermitidas(DELIVERY_LOCAL, ENTREGADA)).isEmpty();

        assertThat(MaquinaEstadosEntrega.siguienteAccion(DELIVERY_LOCAL, PENDIENTE_ASIGNACION)).isEqualTo(AccionEntrega.ASIGNAR);
        assertThat(MaquinaEstadosEntrega.siguienteAccion(DELIVERY_LOCAL, ASIGNADA)).isEqualTo(AccionEntrega.EN_RUTA);
        assertThat(MaquinaEstadosEntrega.siguienteAccion(DELIVERY_LOCAL, EN_RUTA)).isEqualTo(AccionEntrega.ENTREGADA);
        assertThat(MaquinaEstadosEntrega.siguienteAccion(DELIVERY_LOCAL, FALLIDA)).isEqualTo(AccionEntrega.REPROGRAMAR);
        assertThat(MaquinaEstadosEntrega.siguienteAccion(AGENCIA, PENDIENTE_ENVIO)).isEqualTo(AccionEntrega.ENVIADA_AGENCIA);
        assertThat(MaquinaEstadosEntrega.siguienteAccion(AGENCIA, ENVIADO_AGENCIA)).isEqualTo(AccionEntrega.DISPONIBLE_AGENCIA);
        assertThat(MaquinaEstadosEntrega.siguienteAccion(AGENCIA, DISPONIBLE_EN_AGENCIA)).isEqualTo(AccionEntrega.ENTREGADA);
        assertThat(MaquinaEstadosEntrega.siguienteAccion(AGENCIA, ENTREGADA)).isNull();
        assertThat(MaquinaEstadosEntrega.siguienteAccion(AGENCIA, DEVUELTA)).isNull();
    }

    @Test
    @DisplayName("CA-6-19 el estado inicial depende de la modalidad")
    void estadoInicial() {
        assertThat(MaquinaEstadosEntrega.estadoInicial(DELIVERY_LOCAL)).isEqualTo(PENDIENTE_ASIGNACION);
        assertThat(MaquinaEstadosEntrega.estadoInicial(AGENCIA)).isEqualTo(PENDIENTE_ENVIO);
    }
}
