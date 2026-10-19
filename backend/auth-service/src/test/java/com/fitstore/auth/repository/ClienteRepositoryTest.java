package com.fitstore.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.pruebas.Datos;
import com.fitstore.auth.pruebas.PruebaRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class ClienteRepositoryTest {

    @Autowired ClienteRepository repositorio;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("CA-0a-06 ClienteRepository inserta y recupera un cliente con auditoría y versión")
    void inserta() {
        Cliente guardado = repositorio.saveAndFlush(Datos.cliente("lucia@correo.pe", "12345678"));

        Cliente leido = repositorio.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getEmail()).isEqualTo("lucia@correo.pe");
        assertThat(leido.getDni()).isEqualTo("12345678");
        assertThat(leido.isActivo()).isTrue();
        assertThat(leido.getCreadoEn()).isNotNull();
        assertThat(leido.getActualizadoEn()).isNotNull();
        assertThat(leido.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 ClienteRepository actualiza el perfil")
    void actualizaPerfil() {
        Cliente c = repositorio.saveAndFlush(Datos.cliente("a@correo.pe", "11111111"));
        c.setNombres("Rosa");
        c.setTelefono("999111222");
        repositorio.saveAndFlush(c);

        Cliente leido = repositorio.findById(c.getId()).orElseThrow();
        assertThat(leido.getNombres()).isEqualTo("Rosa");
        assertThat(leido.getTelefono()).isEqualTo("999111222");
    }

    @Test
    @DisplayName("CA-0a-06 ClienteRepository elimina de forma lógica: activo=false y la fila sigue")
    void eliminaLogicamente() {
        Cliente c = repositorio.saveAndFlush(Datos.cliente("b@correo.pe", "22222222"));
        c.setActivo(false);
        repositorio.saveAndFlush(c);

        assertThat(repositorio.findById(c.getId())).get().extracting(Cliente::isActivo).isEqualTo(false);
        assertThat(repositorio.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("CA-0a-06 ClienteRepository lista paginado")
    void listaPaginado() {
        for (int i = 0; i < 5; i++) {
            repositorio.save(Datos.cliente("c" + i + "@correo.pe", "3000000" + i));
        }
        repositorio.flush();

        var pagina = repositorio.findAll(PageRequest.of(1, 2, Sort.by("email")));
        assertThat(pagina.getTotalElements()).isEqualTo(5);
        assertThat(pagina.getTotalPages()).isEqualTo(3);
        assertThat(pagina.getContent()).extracting(Cliente::getEmail).containsExactly("c2@correo.pe", "c3@correo.pe");
    }

    @Test
    @DisplayName("CA-0a-06 ClienteRepository busca por nombre, correo o DNI sin importar mayúsculas")
    void busca() {
        Cliente ana = Datos.cliente("ana.torres@correo.pe", "44444444");
        ana.setNombres("Ana");
        ana.setApellidos("Torres");
        repositorio.save(ana);
        repositorio.save(Datos.cliente("otro@correo.pe", "55555555"));
        repositorio.flush();
        var pagina = PageRequest.of(0, 10);

        assertThat(repositorio.buscar("TORRES", pagina).getContent()).extracting(Cliente::getDni).containsExactly("44444444");
        assertThat(repositorio.buscar("ana.torres@", pagina).getContent()).hasSize(1);
        assertThat(repositorio.buscar("5555555", pagina).getContent()).extracting(Cliente::getEmail).containsExactly("otro@correo.pe");
        assertThat(repositorio.buscar("no-existe", pagina).getContent()).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 ClienteRepository consulta por correo sin importar mayúsculas y por DNI")
    void consultasPorCorreoYDni() {
        repositorio.saveAndFlush(Datos.cliente("mixto@correo.pe", "66666666"));

        assertThat(repositorio.existsByEmailIgnoreCase("MIXTO@Correo.PE")).isTrue();
        assertThat(repositorio.findByEmailIgnoreCase("Mixto@correo.pe")).isPresent();
        assertThat(repositorio.existsByDni("66666666")).isTrue();
        assertThat(repositorio.existsByDni("00000000")).isFalse();
    }

    @Test
    @DisplayName("CA-0a-06 ClienteRepository rechaza un correo duplicado sin importar mayúsculas")
    void rechazaCorreoDuplicado() {
        repositorio.saveAndFlush(Datos.cliente("dup@correo.pe", "77777777"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.cliente("DUP@correo.PE", "88888888")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ClienteRepository rechaza un DNI duplicado")
    void rechazaDniDuplicado() {
        repositorio.saveAndFlush(Datos.cliente("uno@correo.pe", "99999999"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.cliente("dos@correo.pe", "99999999")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 ClienteRepository rechaza un DNI que no tiene 8 dígitos (CHECK)")
    void rechazaDniInvalido() {
        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.cliente("x@correo.pe", "1234567A")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
