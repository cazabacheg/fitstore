package com.fitstore.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fitstore.common.storage.FileStorage;
import com.fitstore.common.storage.TipoBucket;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.BoletaApiBase;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Si Garage o settings fallan la boleta no se emite, el pago sigue confirmado y se reintenta sin dejar huecos (CA-5-28). */
class BoletaGarageCaidoTest extends BoletaApiBase {

    @MockitoBean FileStorage almacenamiento;

    @Autowired BoletaService boletas;

    private final UUID clienta = UUID.randomUUID();

    private int ultimoCorrelativo() {
        return jdbc.queryForObject("SELECT ultimo FROM correlativo_boleta WHERE serie = 'B001'", Integer.class);
    }

    @Test
    @DisplayName("CA-5-28 con Garage caído el pago sigue confirmado, no se consume el correlativo y la boleta se genera al reintentar")
    void garageCaido() {
        aislarPendientes();
        IntencionPago i = intencionPagada(clienta);
        doThrow(new IllegalStateException("Garage no responde"))
                .when(almacenamiento).guardar(eq(TipoBucket.PRIVADO), any(), any(), any());
        int antes = ultimoCorrelativo();

        assertThatThrownBy(() -> boletas.emitir(i.getId())).isInstanceOf(IllegalStateException.class);
        assertThat(boletas.emitirPendientes()).isZero();

        assertThat(estadoIntencion(i.getId())).isEqualTo("PAGADA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM boleta WHERE intencion_id = ?", Integer.class, i.getId())).isZero();
        assertThat(ultimoCorrelativo()).isEqualTo(antes);
        assertThat(eventosEnOutbox(i.getPedidoId(), "BoletaEmitida")).isZero();

        // Garage vuelve: el siguiente intento la genera con el primer correlativo libre
        doNothing().when(almacenamiento).guardar(eq(TipoBucket.PRIVADO), any(), any(), any());
        assertThat(boletas.emitirPendientes()).isGreaterThanOrEqualTo(1);

        assertThat(numeroDeBoleta(i.getId())).isEqualTo("B001-%08d".formatted(antes + 1));
        assertThat(eventosEnOutbox(i.getPedidoId(), "BoletaEmitida")).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-5-28 si settings no responde (sin datos del emisor) la boleta se reintenta más tarde")
    void settingsCaido() {
        IntencionPago i = intencionPagada(clienta);
        when(tienda.publica()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> boletas.emitir(i.getId())).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM boleta WHERE intencion_id = ?", Integer.class, i.getId())).isZero();
    }
}
