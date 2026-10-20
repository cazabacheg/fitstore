package com.fitstore.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.MotivoToken;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import com.fitstore.auth.model.TokenPassword;
import com.fitstore.auth.pruebas.Datos;
import com.fitstore.auth.pruebas.PruebaRepositorio;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@PruebaRepositorio
class TokenPasswordRepositoryTest {

    @Autowired TokenPasswordRepository repositorio;
    @Autowired ClienteRepository clientes;
    @Autowired StaffRepository staffs;

    Cliente cliente;
    Staff staff;

    @BeforeEach
    void cuentas() {
        cliente = clientes.saveAndFlush(Datos.cliente("tok@correo.pe", "12121212"));
        staff = staffs.saveAndFlush(Datos.staff("tok@fitstore.pe", RolStaff.OPERADOR));
    }

    private TokenPassword tokenDeCliente(String hash, Instant expiraEn) {
        TokenPassword t = new TokenPassword();
        t.setCliente(cliente);
        t.setTokenHash(hash);
        t.setMotivo(MotivoToken.RECUPERACION);
        t.setExpiraEn(expiraEn);
        return t;
    }

    private TokenPassword tokenDeStaff(String hash, Instant expiraEn) {
        TokenPassword t = new TokenPassword();
        t.setStaff(staff);
        t.setTokenHash(hash);
        t.setMotivo(MotivoToken.INVITACION);
        t.setExpiraEn(expiraEn);
        return t;
    }

    private static String hash(char c) {
        return String.valueOf(c).repeat(64);
    }

    @Test
    @DisplayName("CA-0a-06 TokenPasswordRepository inserta tokens de cliente y de staff")
    void inserta() {
        Instant expira = Instant.now().plus(Duration.ofMinutes(30));
        TokenPassword deCliente = repositorio.saveAndFlush(tokenDeCliente(hash('a'), expira));
        TokenPassword deStaff = repositorio.saveAndFlush(tokenDeStaff(hash('b'), expira));

        TokenPassword c = repositorio.findById(deCliente.getId()).orElseThrow();
        assertThat(c.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(c.getStaff()).isNull();
        assertThat(c.getMotivo()).isEqualTo(MotivoToken.RECUPERACION);
        assertThat(c.getUsadoEn()).isNull();
        assertThat(c.getCreadoEn()).isNotNull();
        TokenPassword s = repositorio.findById(deStaff.getId()).orElseThrow();
        assertThat(s.getStaff().getId()).isEqualTo(staff.getId());
        assertThat(s.getCliente()).isNull();
        assertThat(s.getMotivo()).isEqualTo(MotivoToken.INVITACION);
    }

    @Test
    @DisplayName("CA-0a-06 TokenPasswordRepository marca un token como usado")
    void marcaUsado() {
        TokenPassword t = repositorio.saveAndFlush(tokenDeCliente(hash('c'), Instant.now().plus(Duration.ofMinutes(30))));
        Instant ahora = Instant.now();
        t.setUsadoEn(ahora);
        repositorio.saveAndFlush(t);

        assertThat(repositorio.findById(t.getId())).get().extracting(TokenPassword::getUsadoEn).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 TokenPasswordRepository lista los tokens vigentes por cuenta (sin usados ni vencidos)")
    void vigentesPorCuenta() {
        Instant ahora = Instant.now();
        repositorio.save(tokenDeCliente(hash('d'), ahora.plus(Duration.ofMinutes(30))));
        TokenPassword usado = tokenDeCliente(hash('e'), ahora.plus(Duration.ofMinutes(30)));
        usado.setUsadoEn(ahora);
        repositorio.save(usado);
        repositorio.save(tokenDeCliente(hash('f'), ahora.minus(Duration.ofMinutes(1))));
        repositorio.save(tokenDeStaff(hash('g'), ahora.plus(Duration.ofHours(24))));
        repositorio.flush();

        assertThat(repositorio.findByClienteIdAndUsadoEnIsNullAndExpiraEnAfter(cliente.getId(), ahora))
                .extracting(TokenPassword::getTokenHash).containsExactly(hash('d'));
        assertThat(repositorio.findByStaffIdAndUsadoEnIsNullAndExpiraEnAfter(staff.getId(), ahora))
                .extracting(TokenPassword::getTokenHash).containsExactly(hash('g'));
    }

    @Test
    @DisplayName("CA-0a-06 TokenPasswordRepository busca por hash")
    void buscaPorHash() {
        repositorio.saveAndFlush(tokenDeCliente(hash('h'), Instant.now().plus(Duration.ofMinutes(30))));

        assertThat(repositorio.findByTokenHash(hash('h'))).isPresent();
        assertThat(repositorio.findByTokenHash(hash('z'))).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 TokenPasswordRepository rechaza un hash duplicado")
    void rechazaHashDuplicado() {
        Instant expira = Instant.now().plus(Duration.ofMinutes(30));
        repositorio.saveAndFlush(tokenDeCliente(hash('i'), expira));

        assertThatThrownBy(() -> repositorio.saveAndFlush(tokenDeStaff(hash('i'), expira)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 TokenPasswordRepository rechaza un token sin dueño (CHECK)")
    void rechazaSinDueno() {
        TokenPassword sinDueno = tokenDeCliente(hash('j'), Instant.now().plus(Duration.ofMinutes(30)));
        sinDueno.setCliente(null);

        assertThatThrownBy(() -> repositorio.saveAndFlush(sinDueno)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 TokenPasswordRepository rechaza un token con dos dueños (CHECK)")
    void rechazaDosDuenos() {
        TokenPassword dosDuenos = tokenDeCliente(hash('k'), Instant.now().plus(Duration.ofMinutes(30)));
        dosDuenos.setStaff(staff);

        assertThatThrownBy(() -> repositorio.saveAndFlush(dosDuenos)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
