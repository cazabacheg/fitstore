package com.fitstore.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.RepartidorDto;
import com.fitstore.delivery.dto.RepartidorRequest;
import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.model.TipoVehiculo;
import com.fitstore.delivery.repository.EntregaRepository;
import com.fitstore.delivery.repository.RepartidorRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reglas del mantenimiento de repartidores (CA-6-16 a CA-6-18). */
@ExtendWith(MockitoExtension.class)
class RepartidorServiceTest {

    @Mock RepartidorRepository repartidores;
    @Mock EntregaRepository entregas;

    RepartidorService servicio;

    @BeforeEach
    void preparar() {
        servicio = new RepartidorService(repartidores, entregas);
    }

    @Test
    @DisplayName("CA-6-16 registra un repartidor en moto con placa")
    void registraConPlaca() {
        when(repartidores.save(any(Repartidor.class))).thenAnswer(i -> i.getArgument(0));

        RepartidorDto creado = servicio.crear(new RepartidorRequest("Juan Pérez", "987654321", TipoVehiculo.MOTO, "ABC-123"));

        assertThat(creado.nombres()).isEqualTo("Juan Pérez");
        assertThat(creado.placa()).isEqualTo("ABC-123");
        assertThat(creado.activo()).isTrue();
    }

    @Test
    @DisplayName("CA-6-16 la placa es obligatoria para moto y auto, no para bicicleta")
    void placaSegunVehiculo() {
        when(repartidores.save(any(Repartidor.class))).thenAnswer(i -> i.getArgument(0));

        for (TipoVehiculo vehiculo : new TipoVehiculo[] {TipoVehiculo.MOTO, TipoVehiculo.AUTO}) {
            assertThatThrownBy(() -> servicio.crear(new RepartidorRequest("Juan Pérez", "987654321", vehiculo, null)))
                    .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                            e -> assertThat(e.errores().get(0).campo()).isEqualTo("placa"));
        }
        RepartidorDto enBicicleta = servicio.crear(new RepartidorRequest("Rosa Díaz", "912345678", TipoVehiculo.BICICLETA, null));
        assertThat(enBicicleta.placa()).isNull();
    }

    @Test
    @DisplayName("CA-6-17 desactivar conserva al repartidor y sus entregas; activar lo devuelve")
    void desactivaYActiva() {
        Repartidor r = Repartidor.builder().id(5L).nombres("Juan Pérez").telefono("987654321")
                .vehiculo(TipoVehiculo.MOTO).placa("ABC-123").build();
        when(repartidores.findById(5L)).thenReturn(Optional.of(r));

        assertThat(servicio.cambiarActivo(5L, false).activo()).isFalse();
        assertThat(servicio.cambiarActivo(5L, true).activo()).isTrue();
        verify(repartidores, never()).delete(any());
    }

    @Test
    @DisplayName("CA-6-18 eliminar un repartidor con entregas da 409 y sugiere desactivarlo")
    void noEliminaConEntregas() {
        when(repartidores.findById(5L)).thenReturn(Optional.of(Repartidor.builder().id(5L).build()));
        when(entregas.existsByRepartidorId(5L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.eliminar(5L))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("desactívalo");
        verify(repartidores, never()).delete(any());
    }

    @Test
    @DisplayName("CA-6-18 sin entregas, el repartidor se elimina")
    void eliminaSinEntregas() {
        Repartidor r = Repartidor.builder().id(5L).build();
        when(repartidores.findById(5L)).thenReturn(Optional.of(r));
        when(entregas.existsByRepartidorId(5L)).thenReturn(false);

        servicio.eliminar(5L);

        verify(repartidores).delete(r);
    }
}
