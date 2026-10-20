package com.fitstore.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.delivery.model.Agencia;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.TarifaEnvio;
import com.fitstore.delivery.pruebas.PruebaRepositorio;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Semilla de {@code V2__semilla.sql} (SPEC-0a I6, micro 3.1): agencias mock y tarifas de demostración. */
@PruebaRepositorio
class SemillaDeliveryTest {

    private static final Set<String> DEPARTAMENTOS_DE_AGENCIA = Set.of(
            "01", "02", "03", "04", "05", "06", "08", "09", "10", "11", "12", "13", "14", "16", "17", "18", "19", "20",
            "21", "22", "23", "24", "25");

    private static final Set<String> PROVINCIAS_DE_LIMA_FUERA_DE_LA_METROPOLITANA = Set.of(
            "1502", "1503", "1504", "1505", "1506", "1507", "1508", "1509", "1510");

    @Autowired
    AgenciaRepository agencias;

    @Autowired
    TarifaEnvioRepository tarifas;

    @Test
    @DisplayName("CA-0a-06 la semilla trae las cuatro agencias mock, activas y en orden")
    void agenciasMock() {
        List<Agencia> activas = agencias.findByActivaTrueOrderByOrdenAsc();

        assertThat(activas).extracting(Agencia::getNombre)
                .containsExactly("Shalom", "Olva Courier", "Marvisur", "Cruz del Sur Cargo");
        assertThat(activas).allSatisfy(a -> assertThat(a.getUrlSeguimiento()).contains("{codigo}"));
    }

    @Test
    @DisplayName("CA-0a-06 la semilla trae el delivery local de Lima Metropolitana, Callao, Cercado y La Victoria")
    void deliveryLocal() {
        List<TarifaEnvio> locales = tarifas.findByModalidad(Modalidad.DELIVERY_LOCAL);

        assertThat(locales).extracting(TarifaEnvio::getUbigeo)
                .containsExactlyInAnyOrder("1501", "07", "150101", "150115");
        assertThat(costo(locales, "1501")).isEqualTo("10.00");
        assertThat(costo(locales, "07")).isEqualTo("12.00");
        assertThat(costo(locales, "150101")).isEqualTo("7.00");
        assertThat(costo(locales, "150115")).isEqualTo("7.00");
        assertThat(locales).allSatisfy(t -> {
            assertThat(t.getPlazoDiasMin()).isEqualTo(1);
            assertThat(t.getPlazoDiasMax()).isEqualTo(2);
            assertThat(t.isActiva()).isTrue();
        });
    }

    @Test
    @DisplayName("CA-0a-06 la semilla de agencia cubre 23 departamentos y las provincias de Lima, sin tarifa para Lima (15)")
    void tarifasDeAgencia() {
        List<TarifaEnvio> deAgencia = tarifas.findByModalidad(Modalidad.AGENCIA);
        Set<String> ubigeos = deAgencia.stream().map(TarifaEnvio::getUbigeo).collect(Collectors.toSet());

        assertThat(ubigeos).hasSize(DEPARTAMENTOS_DE_AGENCIA.size() + PROVINCIAS_DE_LIMA_FUERA_DE_LA_METROPOLITANA.size());
        assertThat(ubigeos).containsAll(DEPARTAMENTOS_DE_AGENCIA);
        assertThat(ubigeos).containsAll(PROVINCIAS_DE_LIMA_FUERA_DE_LA_METROPOLITANA);
        assertThat(ubigeos).doesNotContain("15", "07", "1501");
        assertThat(deAgencia).allSatisfy(t -> {
            assertThat(t.getCosto()).isBetween(new java.math.BigDecimal("15.00"), new java.math.BigDecimal("25.00"));
            assertThat(t.getPlazoDiasMin()).isGreaterThanOrEqualTo(1);
            assertThat(t.getPlazoDiasMax()).isLessThanOrEqualTo(5);
        });
    }

    @Test
    @DisplayName("CA-0a-06 Arequipa se cotiza por departamento a S/ 18.00 en 2 a 3 días (ejemplo del micro 4.2)")
    void arequipaPorDepartamento() {
        TarifaEnvio arequipa = tarifas.masEspecifica(Modalidad.AGENCIA, "040101", "0401", "04").orElseThrow();

        assertThat(arequipa.getUbigeo()).isEqualTo("04");
        assertThat(arequipa.getCosto()).isEqualByComparingTo("18.00");
        assertThat(arequipa.getPlazoDiasMin()).isEqualTo(2);
        assertThat(arequipa.getPlazoDiasMax()).isEqualTo(3);
    }

    @Test
    @DisplayName("CA-0a-06 un distrito de Lima Metropolitana sin tarifa propia usa la de la provincia y Lima no tiene agencia")
    void limaMetropolitana() {
        TarifaEnvio miraflores = tarifas.masEspecifica(Modalidad.DELIVERY_LOCAL, "150122", "1501", "15").orElseThrow();

        assertThat(miraflores.getUbigeo()).isEqualTo("1501");
        assertThat(tarifas.masEspecifica(Modalidad.AGENCIA, "150122", "1501", "15")).isEmpty();
    }

    private static String costo(List<TarifaEnvio> tarifas, String ubigeo) {
        return tarifas.stream().filter(t -> t.getUbigeo().equals(ubigeo)).findFirst().orElseThrow()
                .getCosto().toPlainString();
    }
}
