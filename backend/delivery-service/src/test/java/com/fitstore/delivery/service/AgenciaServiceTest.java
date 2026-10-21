package com.fitstore.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.AgenciaDto;
import com.fitstore.delivery.dto.AgenciaRequest;
import com.fitstore.delivery.model.Agencia;
import com.fitstore.delivery.repository.AgenciaRepository;
import com.fitstore.delivery.repository.EntregaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reglas del mantenimiento de agencias (CA-6-13 a CA-6-15). */
@ExtendWith(MockitoExtension.class)
class AgenciaServiceTest {

    @Mock AgenciaRepository agencias;
    @Mock EntregaRepository entregas;

    AgenciaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AgenciaService(agencias, entregas);
    }

    private static Agencia agencia(long id, String nombre, int orden, boolean activa) {
        return Agencia.builder().id(id).nombre(nombre).orden(orden).activa(activa).build();
    }

    @Test
    @DisplayName("CA-6-13 guarda una agencia con nombre único y una URL https con {codigo}")
    void guardaConUrlValida() {
        when(agencias.existsByNombreIgnoreCase("Shalom Express")).thenReturn(false);
        when(agencias.findAllByOrderByOrdenAscIdAsc()).thenReturn(List.of(agencia(1, "Shalom", 3, true)));
        when(agencias.save(any(Agencia.class))).thenAnswer(i -> i.getArgument(0));

        AgenciaDto creada = servicio.crear(new AgenciaRequest("Shalom Express", "https://rastreo.shalom.pe/?codigo={codigo}"));

        assertThat(creada.nombre()).isEqualTo("Shalom Express");
        assertThat(creada.urlSeguimiento()).contains("{codigo}");
        assertThat(creada.activa()).isTrue();
        assertThat(creada.orden()).isEqualTo(4);
    }

    @Test
    @DisplayName("CA-6-13 una URL sin {codigo} o sin https se rechaza con 400")
    void rechazaUrlInvalida() {
        assertThatThrownBy(() -> servicio.crear(new AgenciaRequest("Nueva", "https://rastreo.pe/")))
                .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                        e -> assertThat(e.errores().get(0).campo()).isEqualTo("urlSeguimiento"));
        assertThatThrownBy(() -> servicio.crear(new AgenciaRequest("Nueva", "http://rastreo.pe/?c={codigo}")))
                .isInstanceOf(SolicitudInvalidaException.class);
        verify(agencias, never()).save(any());
    }

    @Test
    @DisplayName("CA-6-13 el nombre repetido da 409")
    void rechazaNombreRepetido() {
        when(agencias.existsByNombreIgnoreCase("shalom")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(new AgenciaRequest("shalom", null)))
                .isInstanceOf(ConflictoNegocioException.class);
    }

    @Test
    @DisplayName("CA-6-14 reordenar mueve la agencia a su posición y renumera todas")
    void reordena() {
        List<Agencia> todas = new ArrayList<>(List.of(agencia(1, "Shalom", 0, true), agencia(2, "Olva", 1, true),
                agencia(3, "Marvisur", 2, true), agencia(4, "Cruz del Sur", 3, true)));
        when(agencias.findById(4L)).thenReturn(Optional.of(todas.get(3)));
        when(agencias.findAllByOrderByOrdenAscIdAsc()).thenReturn(todas);

        List<AgenciaDto> resultado = servicio.ordenar(4L, 0);

        assertThat(resultado).extracting(AgenciaDto::nombre).containsExactly("Cruz del Sur", "Shalom", "Olva", "Marvisur");
        assertThat(resultado).extracting(AgenciaDto::orden).containsExactly(0, 1, 2, 3);
    }

    @Test
    @DisplayName("CA-6-14 desactivar una agencia la deja inactiva")
    void desactiva() {
        Agencia a = agencia(2, "Olva", 1, true);
        when(agencias.findById(2L)).thenReturn(Optional.of(a));

        assertThat(servicio.cambiarActiva(2L, false).activa()).isFalse();
        assertThat(a.isActiva()).isFalse();
    }

    @Test
    @DisplayName("CA-6-15 eliminar una agencia con entregas da 409 y sugiere desactivarla")
    void noEliminaConEntregas() {
        when(agencias.findById(2L)).thenReturn(Optional.of(agencia(2, "Olva", 1, true)));
        when(entregas.existsByAgenciaId(2L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.eliminar(2L))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("desactívala");
        verify(agencias, never()).delete(any());
    }

    @Test
    @DisplayName("CA-6-15 sin entregas, la agencia se elimina")
    void eliminaSinEntregas() {
        Agencia a = agencia(2, "Olva", 1, true);
        when(agencias.findById(2L)).thenReturn(Optional.of(a));
        when(entregas.existsByAgenciaId(2L)).thenReturn(false);

        servicio.eliminar(2L);

        verify(agencias).delete(a);
    }
}
