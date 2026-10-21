package com.fitstore.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.RecursoNoEncontradoException;
import com.fitstore.delivery.config.PropiedadesDelivery;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.TarifaDto;
import com.fitstore.delivery.dto.TarifaRequest;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.NivelTarifa;
import com.fitstore.delivery.model.TarifaEnvio;
import com.fitstore.delivery.repository.TarifaEnvioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reglas del mantenimiento de tarifas (CA-6-07 a CA-6-10). */
@ExtendWith(MockitoExtension.class)
class TarifaServiceTest {

    @Mock TarifaEnvioRepository tarifas;

    TarifaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new TarifaService(tarifas, new PropiedadesDelivery(new PropiedadesDelivery.Local(List.of("1501", "07"))));
    }

    private static TarifaRequest pedido(Modalidad modalidad, String ubigeo, String costo, int min, int max) {
        return new TarifaRequest(modalidad, ubigeo, new BigDecimal(costo), min, max, null);
    }

    @Test
    @DisplayName("CA-6-07 guarda la tarifa con el código de 2, 4 o 6 dígitos y deduce el nivel")
    void guardaConElNivel() {
        when(tarifas.existsByModalidadAndUbigeo(any(), any())).thenReturn(false);
        when(tarifas.save(any(TarifaEnvio.class))).thenAnswer(i -> i.getArgument(0));

        TarifaDto departamento = servicio.crear(pedido(Modalidad.AGENCIA, "04", "18.00", 2, 3));
        TarifaDto provincia = servicio.crear(pedido(Modalidad.AGENCIA, "0801", "16.00", 2, 3));
        TarifaDto distrito = servicio.crear(pedido(Modalidad.DELIVERY_LOCAL, "150122", "9.00", 1, 2));

        assertThat(departamento.nivel()).isEqualTo(NivelTarifa.DEPARTAMENTO);
        assertThat(provincia.nivel()).isEqualTo(NivelTarifa.PROVINCIA);
        assertThat(distrito.nivel()).isEqualTo(NivelTarifa.DISTRITO);
        assertThat(distrito.activa()).isTrue();
    }

    @Test
    @DisplayName("CA-6-08 repetir modalidad y destino da 409 'Ya existe una tarifa para ese destino'")
    void rechazaElDuplicado() {
        when(tarifas.existsByModalidadAndUbigeo(Modalidad.AGENCIA, "04")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(pedido(Modalidad.AGENCIA, "04", "18.00", 2, 3)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessage("Ya existe una tarifa para ese destino");
        verify(tarifas, never()).save(any());
    }

    @Test
    @DisplayName("CA-6-09 el delivery local fuera de Lima Metropolitana y Callao da 400 en el campo ubigeo")
    void localSoloEnLimaYCallao() {
        assertThatThrownBy(() -> servicio.crear(pedido(Modalidad.DELIVERY_LOCAL, "04", "9.00", 1, 2)))
                .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                        e -> assertThat(e.errores().get(0).campo()).isEqualTo("ubigeo"));
        // Lima departamento (15) incluye provincias fuera de Lima Metropolitana
        assertThatThrownBy(() -> servicio.crear(pedido(Modalidad.DELIVERY_LOCAL, "15", "9.00", 1, 2)))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.crear(pedido(Modalidad.DELIVERY_LOCAL, "1502", "9.00", 1, 2)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("CA-6-09 un plazo mínimo mayor que el máximo da 400 en el campo plazoDiasMax")
    void plazoMinimoNoMayorQueElMaximo() {
        assertThatThrownBy(() -> servicio.crear(pedido(Modalidad.AGENCIA, "04", "18.00", 5, 3)))
                .isInstanceOfSatisfying(SolicitudInvalidaException.class,
                        e -> assertThat(e.errores().get(0).campo()).isEqualTo("plazoDiasMax"));
    }

    @Test
    @DisplayName("CA-6-09 el ubigeo debe tener 2, 4 o 6 dígitos")
    void ubigeoConNivelValido() {
        assertThatThrownBy(() -> servicio.crear(pedido(Modalidad.AGENCIA, "040", "18.00", 1, 2)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("CA-6-10 desactivar una tarifa la deja inactiva, sin borrarla")
    void desactivar() {
        TarifaEnvio existente = TarifaEnvio.builder().id(7L).modalidad(Modalidad.AGENCIA).ubigeo("04")
                .costo(new BigDecimal("18.00")).plazoDiasMin(2).plazoDiasMax(3).activa(true).build();
        when(tarifas.findById(7L)).thenReturn(Optional.of(existente));
        when(tarifas.existsByModalidadAndUbigeoAndIdNot(Modalidad.AGENCIA, "04", 7L)).thenReturn(false);
        when(tarifas.save(existente)).thenReturn(existente);

        TarifaDto editada = servicio.editar(7L, new TarifaRequest(Modalidad.AGENCIA, "04", new BigDecimal("18.00"), 2, 3, false));

        assertThat(editada.activa()).isFalse();
        assertThat(existente.isActiva()).isFalse();
    }

    @Test
    @DisplayName("CA-6-10 eliminar una tarifa inexistente da 404")
    void eliminarInexistente() {
        when(tarifas.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.eliminar(5L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("CA-6-10 eliminar borra físicamente la tarifa")
    void eliminar() {
        TarifaEnvio existente = TarifaEnvio.builder().id(8L).modalidad(Modalidad.AGENCIA).ubigeo("04").build();
        when(tarifas.findById(8L)).thenReturn(Optional.of(existente));

        servicio.eliminar(8L);

        verify(tarifas).delete(existente);
    }
}
