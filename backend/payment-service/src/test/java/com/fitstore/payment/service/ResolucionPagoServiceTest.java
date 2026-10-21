package com.fitstore.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.payment.model.EstadoIntencion;
import com.fitstore.payment.model.EstadoTransaccion;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.model.Proveedor;
import com.fitstore.payment.model.Transaccion;
import com.fitstore.payment.provider.ResultadoCargo;
import com.fitstore.payment.pruebas.PagoApiBase;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Cómo se aplica lo que informa la pasarela a una transacción en curso (webhook y verificador comparten esta lógica). */
class ResolucionPagoServiceTest extends PagoApiBase {

    @Autowired ResolucionPagoService resolucion;

    private final UUID clienta = UUID.randomUUID();

    private static ResultadoCargo aprobado(String id) {
        return new ResultadoCargo(EstadoTransaccion.APROBADA, id, "accredited", "¡Pago aprobado!", "Visa ****1111", Map.of());
    }

    @Test
    @DisplayName("CA-5-20 una transacción que ya no está en curso no se vuelve a resolver")
    void yaResuelta() {
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, "1", 5);
        assertThat(resolucion.resolver(t.getId(), aprobado("1"))).isTrue();

        assertThat(resolucion.resolver(t.getId(), aprobado("1"))).isFalse();
        assertThat(resolucion.resolver(UUID.randomUUID(), aprobado("1"))).isFalse();
        assertThat(eventosEnOutbox(i.getPedidoId(), "PagoConfirmado")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-20 si la pasarela sigue informando \"en proceso\" la transacción no cambia")
    void sigueEnProceso() {
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, "2", 5);

        boolean cambio = resolucion.resolver(t.getId(), new ResultadoCargo(EstadoTransaccion.PROCESANDO, "2", "pending",
                "Estamos confirmando tu pago", null, Map.of()));

        assertThat(cambio).isFalse();
        assertThat(estadoTransaccion(t.getId())).isEqualTo("PROCESANDO");
    }

    @Test
    @DisplayName("CA-5-20 un cobro aprobado sobre un pedido ya anulado no lo confirma: queda en ERROR para revisión")
    void aprobadoTardeSobreUnPedidoAnulado() {
        IntencionPago i = intencion(clienta);
        Transaccion t = transaccionProcesando(i, Proveedor.MERCADO_PAGO, "3", 5);
        i.setEstado(EstadoIntencion.ANULADA);
        intenciones.saveAndFlush(i);

        resolucion.resolver(t.getId(), aprobado("3"));

        assertThat(estadoTransaccion(t.getId())).isEqualTo("ERROR");
        assertThat(estadoIntencion(i.getId())).isEqualTo("ANULADA");
        assertThat(eventosEnOutbox(i.getPedidoId(), "PagoConfirmado")).isZero();
    }

    @Test
    @DisplayName("CA-5-20 un aviso de un pago que no corresponde a ninguna transacción se ignora")
    void pagoDesconocido() {
        ResultadoCargo sinReferencia = new ResultadoCargo(EstadoTransaccion.APROBADA, "999", "x", "x", null, Map.of());
        ResultadoCargo referenciaRota = new ResultadoCargo(EstadoTransaccion.APROBADA, "999", "x", "x", null,
                Map.of("external_reference", "no-es-un-uuid"));
        ResultadoCargo referenciaAjena = new ResultadoCargo(EstadoTransaccion.APROBADA, "999", "x", "x", null,
                Map.of("external_reference", UUID.randomUUID().toString()));

        assertThat(resolucion.resolverPorPago(Proveedor.MERCADO_PAGO, "999", sinReferencia)).isFalse();
        assertThat(resolucion.resolverPorPago(Proveedor.MERCADO_PAGO, "999", referenciaRota)).isFalse();
        assertThat(resolucion.resolverPorPago(Proveedor.MERCADO_PAGO, "999", referenciaAjena)).isFalse();
    }

    @Test
    @DisplayName("CA-5-22 marcar como ERROR solo afecta a lo que sigue en curso")
    void marcarError() {
        IntencionPago i = intencion(clienta);
        Transaccion enCurso = transaccionProcesando(i, Proveedor.MERCADO_PAGO, null, 30);
        IntencionPago otra = intencion(clienta);
        Transaccion aprobada = transaccionProcesando(otra, Proveedor.MERCADO_PAGO, "4", 30);
        resolucion.resolver(aprobada.getId(), aprobado("4"));

        resolucion.marcarError(enCurso.getId(), "La pasarela no registró el cobro");
        resolucion.marcarError(aprobada.getId(), "no debería cambiar");
        resolucion.marcarError(UUID.randomUUID(), "no existe");

        assertThat(estadoTransaccion(enCurso.getId())).isEqualTo("ERROR");
        assertThat(estadoTransaccion(aprobada.getId())).isEqualTo("APROBADA");
    }
}
