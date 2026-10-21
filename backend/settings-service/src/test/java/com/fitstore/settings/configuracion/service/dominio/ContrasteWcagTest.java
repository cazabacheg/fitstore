package com.fitstore.settings.configuracion.service.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** HU-3-03: el contraste advierte pero nunca bloquea (CA-3-10). */
class ContrasteWcagTest {

    @Test
    @DisplayName("CA-3-10 negro sobre blanco es 21:1")
    void negroSobreBlanco() {
        assertThat(ContrasteWcag.razon("#000000", "#FFFFFF")).isCloseTo(21.0, within(0.01));
    }

    @Test
    @DisplayName("CA-3-10 la razón no depende del orden de los colores ni de las mayúsculas")
    void simetricaEInsensibleAMayusculas() {
        assertThat(ContrasteWcag.razon("#ffffff", "#000000")).isCloseTo(21.0, within(0.01));
        assertThat(ContrasteWcag.razon("#c9b28c", "#26221d"))
                .isEqualTo(ContrasteWcag.razon("#26221D", "#C9B28C"));
    }

    @Test
    @DisplayName("CA-3-10 dos colores iguales dan 1:1")
    void coloresIguales() {
        assertThat(ContrasteWcag.razon("#C9B28C", "#C9B28C")).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("CA-3-10 gris #767676 sobre blanco es el límite conocido de 4.54:1")
    void limiteConocido() {
        assertThat(ContrasteWcag.razon("#767676", "#FFFFFF")).isCloseTo(4.54, within(0.01));
    }

    @Test
    @DisplayName("CA-3-10 cumple AA desde 4.5:1")
    void cumpleAA() {
        assertThat(ContrasteWcag.cumpleAA(4.5)).isTrue();
        assertThat(ContrasteWcag.cumpleAA(4.49)).isFalse();
        assertThat(ContrasteWcag.cumpleAA(21)).isTrue();
    }

    @Test
    @DisplayName("CA-3-10 los colores de FitStore (texto sobre fondo) cumplen AA")
    void temaPorDefectoCumple() {
        assertThat(ContrasteWcag.cumpleAA(ContrasteWcag.razon("#26221D", "#FAF9F7"))).isTrue();
    }

    @Test
    @DisplayName("CA-3-10 sobre el primario dorado el color de texto sí llega aunque el blanco no")
    void primarioConTextoOscuro() {
        assertThat(ContrasteWcag.cumpleAA(ContrasteWcag.razon("#C9B28C", "#FFFFFF"))).isFalse();
        assertThat(ContrasteWcag.textoLegibleSobrePrimario("#C9B28C", "#26221D")).isTrue();
    }

    @Test
    @DisplayName("CA-3-10 un primario sobre el que ni el blanco ni el texto llegan a 4.5:1 se advierte")
    void primarioSinTextoLegible() {
        assertThat(ContrasteWcag.textoLegibleSobrePrimario("#808080", "#777777")).isFalse();
        assertThat(ContrasteWcag.textoLegibleSobrePrimario("#1E3A8A", "#FFFFFF")).isTrue();
    }
}
