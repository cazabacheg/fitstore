package com.fitstore.settings.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.settings.pruebas.PruebaDatos;
import com.fitstore.settings.ubigeo.model.Departamento;
import com.fitstore.settings.ubigeo.model.Distrito;
import com.fitstore.settings.ubigeo.model.Provincia;
import com.fitstore.settings.ubigeo.repository.DepartamentoRepository;
import com.fitstore.settings.ubigeo.repository.DistritoRepository;
import com.fitstore.settings.ubigeo.repository.ProvinciaRepository;
import com.fitstore.settings.ubigeo.repository.UbigeoRepository;
import com.fitstore.settings.ubigeo.repository.UbigeoRepositoryImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Las consultas se prueban con datos propios de la prueba. Los conteos y el contenido de la migración
 * {@code V2__ubigeo_inei.sql} se verifican aparte, cuando el autor defina la fuente oficial del INEI.
 */
@Import(UbigeoRepositoryImpl.class)
class UbigeoRepositoryTest extends PruebaDatos {

    @Autowired DepartamentoRepository departamentos;
    @Autowired ProvinciaRepository provincias;
    @Autowired DistritoRepository distritos;
    @Autowired UbigeoRepository ubigeo;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void datosDePrueba() {
        jdbc.update("DELETE FROM distrito");
        jdbc.update("DELETE FROM provincia");
        jdbc.update("DELETE FROM departamento");
        jdbc.update("INSERT INTO departamento (codigo, nombre) VALUES ('15','Lima'),('04','Arequipa'),('10','Huánuco')");
        jdbc.update("INSERT INTO provincia (codigo, departamento_codigo, nombre) VALUES "
                + "('1501','15','Lima'),('1502','15','Barranca'),('0401','04','Arequipa'),('1001','10','Huánuco')");
        jdbc.update("INSERT INTO distrito (codigo, provincia_codigo, nombre) VALUES "
                + "('150122','1501','Miraflores'),('150101','1501','Lima'),('040117','0401','Miraflores'),"
                + "('100101','1001','Huánuco'),('100102','1001','Amarilis')");
    }

    @Test
    @DisplayName("CA-0a-06 lista los departamentos ordenados por nombre")
    void listaDepartamentos() {
        assertThat(departamentos.findAllByOrderByNombreAsc()).extracting(Departamento::getNombre)
                .containsExactly("Arequipa", "Huánuco", "Lima");
        assertThat(ubigeo.departamentos()).hasSize(3);
    }

    @Test
    @DisplayName("CA-0a-06 lista las provincias de un departamento")
    void provinciasPorDepartamento() {
        assertThat(provincias.findByDepartamentoCodigoOrderByNombreAsc("15")).extracting(Provincia::getCodigo)
                .containsExactly("1502", "1501");
        assertThat(ubigeo.provinciasDe("04")).extracting(Provincia::getNombre).containsExactly("Arequipa");
        assertThat(ubigeo.provinciasDe("99")).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 lista los distritos de una provincia")
    void distritosPorProvincia() {
        assertThat(distritos.findByProvinciaCodigoOrderByNombreAsc("1001")).extracting(Distrito::getCodigo)
                .containsExactly("100102", "100101");
        assertThat(ubigeo.distritosDe("1501")).extracting(Distrito::getNombre).containsExactly("Lima", "Miraflores");
    }

    @Test
    @DisplayName("CA-0a-06 busca distritos sin importar tildes ni mayúsculas")
    void busquedaSinTildes() {
        assertThat(ubigeo.buscarDistritos("huanuco")).extracting(Distrito::getCodigo).containsExactly("100101");
        assertThat(ubigeo.buscarDistritos("HUÁNUCO")).extracting(Distrito::getCodigo).containsExactly("100101");
        assertThat(ubigeo.buscarDistritos("amari")).extracting(Distrito::getCodigo).containsExactly("100102");
    }

    @Test
    @DisplayName("CA-0a-06 el buscador distingue homónimos por su provincia y departamento")
    void homonimos() {
        List<Distrito> encontrados = ubigeo.buscarDistritos("miraf");

        assertThat(encontrados).hasSize(2);
        assertThat(encontrados).extracting(d -> d.getProvincia().getDepartamento().getNombre())
                .containsExactlyInAnyOrder("Lima", "Arequipa");
    }

    @Test
    @DisplayName("CA-0a-06 el nombre completo de un código se arma como 'Distrito, Provincia, Departamento'")
    void nombreCompleto() {
        assertThat(ubigeo.nombreCompleto("150122")).contains("Miraflores, Lima, Lima");
        assertThat(ubigeo.nombreCompleto("040117")).contains("Miraflores, Arequipa, Arequipa");
        assertThat(ubigeo.nombreCompleto("999999")).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 una provincia sin departamento existente viola la clave foránea")
    void provinciaHuerfana() {
        Provincia huerfana = new Provincia();
        huerfana.setCodigo("9901");
        huerfana.setDepartamento(departamentoInexistente());
        huerfana.setNombre("Huérfana");

        assertThatThrownBy(() -> {
            provincias.saveAndFlush(huerfana);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Departamento departamentoInexistente() {
        Departamento d = new Departamento();
        d.setCodigo("99");
        d.setNombre("No existe");
        return d;
    }
}
