package com.fitstore.delivery.repository;

import static com.fitstore.delivery.pruebas.Datos.tarifa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.TarifaEnvio;
import com.fitstore.delivery.pruebas.PruebaRepositorio;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@PruebaRepositorio
class TarifaEnvioRepositoryTest {

    @Autowired
    TarifaEnvioRepository repositorio;

    @Autowired
    TestEntityManager em;

    @BeforeEach
    void sinSemilla() {
        // V2 siembra tarifas: cada caso parte de una tabla vacía (la transacción se revierte al terminar)
        repositorio.deleteAllInBatch();
    }

    private TarifaEnvio guardar(Modalidad modalidad, String ubigeo, String costo) {
        TarifaEnvio guardada = repositorio.saveAndFlush(tarifa(modalidad, ubigeo, costo));
        em.clear();
        return guardada;
    }

    @Test
    @DisplayName("CA-0a-06 inserta tarifas de los tres niveles: departamento, provincia y distrito")
    void insertaLosTresNiveles() {
        guardar(Modalidad.AGENCIA, "04", "18.00");
        guardar(Modalidad.AGENCIA, "0401", "16.00");
        guardar(Modalidad.AGENCIA, "040101", "15.00");

        assertThat(repositorio.findAll()).extracting(TarifaEnvio::getUbigeo)
                .containsExactlyInAnyOrder("04", "0401", "040101");
        TarifaEnvio distrito = repositorio.findAll().stream().filter(t -> t.getUbigeo().length() == 6).findFirst().orElseThrow();
        assertThat(distrito.getId()).isNotNull();
        assertThat(distrito.isActiva()).isTrue();
        assertThat(distrito.getCreadoEn()).isNotNull();
        assertThat(distrito.getActualizadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 actualiza el costo y el plazo de una tarifa")
    void actualizaCostoYPlazo() {
        Long id = guardar(Modalidad.AGENCIA, "04", "18.00").getId();

        TarifaEnvio tarifa = repositorio.findById(id).orElseThrow();
        tarifa.setCosto(new BigDecimal("21.50"));
        tarifa.setPlazoDiasMin(3);
        tarifa.setPlazoDiasMax(6);
        repositorio.saveAndFlush(tarifa);
        em.clear();

        TarifaEnvio releida = repositorio.findById(id).orElseThrow();
        assertThat(releida.getCosto()).isEqualByComparingTo("21.50");
        assertThat(releida.getPlazoDiasMin()).isEqualTo(3);
        assertThat(releida.getPlazoDiasMax()).isEqualTo(6);
    }

    @Test
    @DisplayName("CA-0a-06 elimina físicamente una tarifa")
    void eliminaFisicamente() {
        Long id = guardar(Modalidad.AGENCIA, "04", "18.00").getId();

        repositorio.deleteById(id);
        repositorio.flush();

        assertThat(repositorio.findById(id)).isEmpty();
        assertThat(repositorio.count()).isZero();
    }

    @Test
    @DisplayName("CA-0a-06 lista las tarifas por modalidad")
    void listaPorModalidad() {
        guardar(Modalidad.AGENCIA, "04", "18.00");
        guardar(Modalidad.AGENCIA, "08", "20.00");
        guardar(Modalidad.DELIVERY_LOCAL, "1501", "10.00");

        assertThat(repositorio.findByModalidad(Modalidad.AGENCIA)).extracting(TarifaEnvio::getUbigeo)
                .containsExactlyInAnyOrder("04", "08");
        assertThat(repositorio.findByModalidad(Modalidad.DELIVERY_LOCAL)).extracting(TarifaEnvio::getUbigeo)
                .containsExactly("1501");
    }

    // --- Búsqueda jerárquica distrito -> provincia -> departamento (micro 4.2 y 10.2) ---

    private java.util.Optional<TarifaEnvio> buscar(Modalidad modalidad, String distrito) {
        return repositorio.masEspecifica(modalidad, distrito, distrito.substring(0, 4), distrito.substring(0, 2));
    }

    @Test
    @DisplayName("CA-0a-06 la búsqueda jerárquica prefiere la tarifa del distrito")
    void jerarquiaPrefiereDistrito() {
        guardar(Modalidad.AGENCIA, "04", "18.00");
        guardar(Modalidad.AGENCIA, "0401", "16.00");
        guardar(Modalidad.AGENCIA, "040101", "15.00");

        TarifaEnvio encontrada = buscar(Modalidad.AGENCIA, "040101").orElseThrow();

        assertThat(encontrada.getUbigeo()).isEqualTo("040101");
        assertThat(encontrada.getCosto()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("CA-0a-06 la búsqueda jerárquica cae a la provincia si el distrito no tiene tarifa")
    void jerarquiaCaeAProvincia() {
        guardar(Modalidad.AGENCIA, "04", "18.00");
        guardar(Modalidad.AGENCIA, "0401", "16.00");

        assertThat(buscar(Modalidad.AGENCIA, "040101").orElseThrow().getUbigeo()).isEqualTo("0401");
    }

    @Test
    @DisplayName("CA-0a-06 la búsqueda jerárquica cae al departamento si no hay distrito ni provincia")
    void jerarquiaCaeADepartamento() {
        guardar(Modalidad.AGENCIA, "04", "18.00");

        TarifaEnvio encontrada = buscar(Modalidad.AGENCIA, "040101").orElseThrow();

        assertThat(encontrada.getUbigeo()).isEqualTo("04");
        assertThat(encontrada.getCosto()).isEqualByComparingTo("18.00");
    }

    @Test
    @DisplayName("CA-0a-06 la búsqueda jerárquica no devuelve nada si ningún nivel tiene tarifa")
    void jerarquiaSinTarifa() {
        guardar(Modalidad.AGENCIA, "08", "20.00");
        guardar(Modalidad.AGENCIA, "0402", "16.00");
        guardar(Modalidad.AGENCIA, "040102", "15.00");

        assertThat(buscar(Modalidad.AGENCIA, "040101")).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 la búsqueda jerárquica ignora las tarifas inactivas")
    void jerarquiaIgnoraInactivas() {
        TarifaEnvio distrito = tarifa(Modalidad.AGENCIA, "040101", "15.00");
        distrito.setActiva(false);
        repositorio.saveAndFlush(distrito);
        guardar(Modalidad.AGENCIA, "0401", "16.00");

        assertThat(buscar(Modalidad.AGENCIA, "040101").orElseThrow().getUbigeo()).isEqualTo("0401");
    }

    @Test
    @DisplayName("CA-0a-06 la búsqueda jerárquica solo mira la modalidad pedida")
    void jerarquiaRespetaModalidad() {
        guardar(Modalidad.DELIVERY_LOCAL, "150115", "7.00");
        guardar(Modalidad.AGENCIA, "15", "17.00");

        assertThat(buscar(Modalidad.DELIVERY_LOCAL, "150115").orElseThrow().getCosto()).isEqualByComparingTo("7.00");
        assertThat(buscar(Modalidad.AGENCIA, "150115").orElseThrow().getUbigeo()).isEqualTo("15");
        assertThat(buscar(Modalidad.DELIVERY_LOCAL, "150122")).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 la búsqueda jerárquica devuelve la más específica aunque se haya insertado después la general")
    void jerarquiaOrdenaPorEspecificidad() {
        guardar(Modalidad.AGENCIA, "040101", "15.00");
        guardar(Modalidad.AGENCIA, "0401", "16.00");
        guardar(Modalidad.AGENCIA, "04", "18.00");

        assertThat(buscar(Modalidad.AGENCIA, "040101").orElseThrow().getUbigeo()).isEqualTo("040101");
        assertThat(buscar(Modalidad.AGENCIA, "040102").orElseThrow().getUbigeo()).isEqualTo("0401");
        assertThat(buscar(Modalidad.AGENCIA, "040201").orElseThrow().getUbigeo()).isEqualTo("04");
    }

    // --- Restricciones ---

    private void ejecutaYFalla(Runnable accion) {
        assertThatThrownBy(accion::run).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza una segunda tarifa con la misma modalidad y ubigeo")
    void rechazaDuplicado() {
        guardar(Modalidad.AGENCIA, "04", "18.00");

        ejecutaYFalla(() -> repositorio.saveAndFlush(tarifa(Modalidad.AGENCIA, "04", "19.00")));
    }

    @Test
    @DisplayName("CA-0a-06 permite el mismo ubigeo en otra modalidad")
    void permiteMismoUbigeoEnOtraModalidad() {
        guardar(Modalidad.AGENCIA, "1501", "17.00");
        guardar(Modalidad.DELIVERY_LOCAL, "1501", "10.00");

        assertThat(repositorio.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un ubigeo de 3 dígitos")
    void rechazaUbigeoDeTresDigitos() {
        ejecutaYFalla(() -> repositorio.saveAndFlush(tarifa(Modalidad.AGENCIA, "040", "18.00")));
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un ubigeo de 5 dígitos")
    void rechazaUbigeoDeCincoDigitos() {
        ejecutaYFalla(() -> repositorio.saveAndFlush(tarifa(Modalidad.AGENCIA, "04010", "18.00")));
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un ubigeo con letras")
    void rechazaUbigeoConLetras() {
        ejecutaYFalla(() -> repositorio.saveAndFlush(tarifa(Modalidad.AGENCIA, "04AB", "18.00")));
    }

    @Test
    @DisplayName("CA-0a-06 rechaza delivery local fuera de Lima Metropolitana y Callao")
    void rechazaLocalFueraDeLimaYCallao() {
        ejecutaYFalla(() -> repositorio.saveAndFlush(tarifa(Modalidad.DELIVERY_LOCAL, "040101", "10.00")));
    }

    @Test
    @DisplayName("CA-0a-06 rechaza delivery local para todo el departamento de Lima (15)")
    void rechazaLocalParaElDepartamentoDeLima() {
        ejecutaYFalla(() -> repositorio.saveAndFlush(tarifa(Modalidad.DELIVERY_LOCAL, "15", "10.00")));
    }

    @Test
    @DisplayName("CA-0a-06 rechaza delivery local en una provincia de Lima fuera de Lima Metropolitana")
    void rechazaLocalEnProvinciaDeLimaNoMetropolitana() {
        ejecutaYFalla(() -> repositorio.saveAndFlush(tarifa(Modalidad.DELIVERY_LOCAL, "150501", "10.00")));
    }

    @Test
    @DisplayName("CA-0a-06 acepta delivery local en Lima Metropolitana y en Callao a los tres niveles")
    void aceptaLocalEnLimaYCallao() {
        guardar(Modalidad.DELIVERY_LOCAL, "1501", "10.00");
        guardar(Modalidad.DELIVERY_LOCAL, "150122", "9.00");
        guardar(Modalidad.DELIVERY_LOCAL, "07", "12.00");
        guardar(Modalidad.DELIVERY_LOCAL, "070101", "11.00");

        assertThat(repositorio.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un costo negativo")
    void rechazaCostoNegativo() {
        ejecutaYFalla(() -> repositorio.saveAndFlush(tarifa(Modalidad.AGENCIA, "04", "-0.01")));
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un plazo mínimo mayor que el máximo")
    void rechazaPlazoInvertido() {
        TarifaEnvio invertida = tarifa(Modalidad.AGENCIA, "04", "18.00");
        invertida.setPlazoDiasMin(5);
        invertida.setPlazoDiasMax(2);

        ejecutaYFalla(() -> repositorio.saveAndFlush(invertida));
    }

    @Test
    @DisplayName("CA-0a-06 rechaza un plazo mínimo negativo")
    void rechazaPlazoMinimoNegativo() {
        TarifaEnvio negativa = tarifa(Modalidad.AGENCIA, "04", "18.00");
        negativa.setPlazoDiasMin(-1);

        ejecutaYFalla(() -> repositorio.saveAndFlush(negativa));
    }
}
