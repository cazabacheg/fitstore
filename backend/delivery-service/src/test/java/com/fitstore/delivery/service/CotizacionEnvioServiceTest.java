package com.fitstore.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fitstore.common.api.FitstoreException;
import com.fitstore.delivery.config.PropiedadesDelivery;
import com.fitstore.delivery.config.SolicitudInvalidaException;
import com.fitstore.delivery.dto.CotizacionEnvioDto;
import com.fitstore.delivery.dto.OpcionEnvioDto;
import com.fitstore.delivery.model.Agencia;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.NivelTarifa;
import com.fitstore.delivery.model.TarifaEnvio;
import com.fitstore.delivery.repository.AgenciaRepository;
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
import org.springframework.http.HttpStatus;

/** Núcleo: cotización con búsqueda jerárquica y sus validaciones (CA-6-01 a CA-6-06). */
@ExtendWith(MockitoExtension.class)
class CotizacionEnvioServiceTest {

    @Mock TarifaEnvioRepository tarifas;
    @Mock AgenciaRepository agencias;

    CotizacionEnvioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CotizacionEnvioService(tarifas, agencias,
                new PropiedadesDelivery(new PropiedadesDelivery.Local(List.of("1501", "07"))));
    }

    private static TarifaEnvio tarifa(Modalidad modalidad, String ubigeo, String costo, int min, int max) {
        return TarifaEnvio.builder().modalidad(modalidad).ubigeo(ubigeo).costo(new BigDecimal(costo))
                .plazoDiasMin(min).plazoDiasMax(max).build();
    }

    @Test
    @DisplayName("CA-6-01 usa la tarifa del departamento cuando no hay del distrito ni de la provincia")
    void usaLaTarifaDelDepartamento() {
        when(tarifas.masEspecifica(Modalidad.AGENCIA, "040101", "0401", "04"))
                .thenReturn(Optional.of(tarifa(Modalidad.AGENCIA, "04", "18.00", 2, 3)));

        CotizacionEnvioDto cotizacion = servicio.cotizar(Modalidad.AGENCIA, "040101", null);

        assertThat(cotizacion.costo()).isEqualByComparingTo("18.00");
        assertThat(cotizacion.plazoDiasMin()).isEqualTo(2);
        assertThat(cotizacion.plazoDiasMax()).isEqualTo(3);
        assertThat(cotizacion.nivelTarifa()).isEqualTo(NivelTarifa.DEPARTAMENTO);
        assertThat(cotizacion.ubigeoTarifa()).isEqualTo("04");
    }

    @Test
    @DisplayName("CA-6-02 gana la tarifa del distrito y, sin ella, la de la provincia")
    void gananLasMasEspecificas() {
        when(tarifas.masEspecifica(Modalidad.DELIVERY_LOCAL, "150115", "1501", "15"))
                .thenReturn(Optional.of(tarifa(Modalidad.DELIVERY_LOCAL, "150115", "7.00", 1, 2)));
        when(tarifas.masEspecifica(Modalidad.DELIVERY_LOCAL, "150122", "1501", "15"))
                .thenReturn(Optional.of(tarifa(Modalidad.DELIVERY_LOCAL, "1501", "10.00", 1, 2)));

        CotizacionEnvioDto laVictoria = servicio.cotizar(Modalidad.DELIVERY_LOCAL, "150115", null);
        CotizacionEnvioDto miraflores = servicio.cotizar(Modalidad.DELIVERY_LOCAL, "150122", null);

        assertThat(laVictoria.nivelTarifa()).isEqualTo(NivelTarifa.DISTRITO);
        assertThat(laVictoria.costo()).isEqualByComparingTo("7.00");
        assertThat(miraflores.nivelTarifa()).isEqualTo(NivelTarifa.PROVINCIA);
        assertThat(miraflores.ubigeoTarifa()).isEqualTo("1501");
        assertThat(miraflores.costo()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("CA-6-03 un ubigeo que no tiene 6 dígitos da 400 'Elige el distrito de destino'")
    void exigeElDistrito() {
        for (String ubigeo : new String[] {null, "", "15", "1501", "15011", "1501221", "15012a"}) {
            assertThatThrownBy(() -> servicio.cotizar(Modalidad.AGENCIA, ubigeo, null))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Elige el distrito de destino");
        }
    }

    @Test
    @DisplayName("CA-6-04 sin tarifa para la modalidad da 422 SIN_COBERTURA")
    void sinCobertura() {
        when(tarifas.masEspecifica(any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cotizar(Modalidad.AGENCIA, "150122", null))
                .isInstanceOfSatisfying(FitstoreException.class, e -> {
                    assertThat(e.estado()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                    assertThat(e.codigo()).isEqualTo("SIN_COBERTURA");
                    assertThat(e.getMessage()).isEqualTo("No realizamos envíos por esta modalidad a este distrito");
                });
    }

    @Test
    @DisplayName("CA-6-05 el delivery local fuera de Lima Metropolitana y Callao da 400")
    void localSoloEnLimaYCallao() {
        assertThatThrownBy(() -> servicio.cotizar(Modalidad.DELIVERY_LOCAL, "040101", null))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.cotizar(Modalidad.DELIVERY_LOCAL, "150201", null))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("CA-6-05 una agencia inexistente o inactiva da 400; una activa pone su nombre")
    void validaLaAgencia() {
        Agencia inactiva = Agencia.builder().id(2L).nombre("Olva Courier").activa(false).build();
        Agencia activa = Agencia.builder().id(1L).nombre("Shalom").activa(true).build();
        when(agencias.findById(99L)).thenReturn(Optional.empty());
        when(agencias.findById(2L)).thenReturn(Optional.of(inactiva));
        when(agencias.findById(1L)).thenReturn(Optional.of(activa));
        when(tarifas.masEspecifica(eq(Modalidad.AGENCIA), any(), any(), any()))
                .thenReturn(Optional.of(tarifa(Modalidad.AGENCIA, "04", "18.00", 2, 3)));

        assertThatThrownBy(() -> servicio.cotizar(Modalidad.AGENCIA, "040101", 99L))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> servicio.cotizar(Modalidad.AGENCIA, "040101", 2L))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThat(servicio.cotizar(Modalidad.AGENCIA, "040101", 1L).agenciaNombre()).isEqualTo("Shalom");
    }

    @Test
    @DisplayName("CA-6-06 las opciones traen solo las modalidades con tarifa, con costo y plazo")
    void opcionesDelDistrito() {
        when(tarifas.masEspecifica(Modalidad.DELIVERY_LOCAL, "150122", "1501", "15"))
                .thenReturn(Optional.of(tarifa(Modalidad.DELIVERY_LOCAL, "1501", "10.00", 1, 2)));
        when(tarifas.masEspecifica(Modalidad.AGENCIA, "150122", "1501", "15")).thenReturn(Optional.empty());

        List<OpcionEnvioDto> opciones = servicio.opciones("150122");

        assertThat(opciones).singleElement().satisfies(o -> {
            assertThat(o.modalidad()).isEqualTo(Modalidad.DELIVERY_LOCAL);
            assertThat(o.costo()).isEqualByComparingTo("10.00");
            assertThat(o.plazoDiasMax()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("CA-6-06 las opciones exigen el distrito de 6 dígitos")
    void opcionesExigenElDistrito() {
        assertThatThrownBy(() -> servicio.opciones("1501")).isInstanceOf(SolicitudInvalidaException.class);
    }
}
