package com.fitstore.settings.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.settings.pruebas.Datos;
import com.fitstore.settings.pruebas.PruebaDatos;
import com.fitstore.settings.reclamaciones.model.EstadoReclamo;
import com.fitstore.settings.reclamaciones.model.Reclamo;
import com.fitstore.settings.reclamaciones.model.TipoReclamo;
import com.fitstore.settings.reclamaciones.repository.ReclamoRepository;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class ReclamoRepositoryTest extends PruebaDatos {

    @Autowired ReclamoRepository repositorio;
    @Autowired EntityManager em;

    @Test
    @DisplayName("CA-0a-06 inserta un reclamo con consumidor y bien, y lo encuentra por número")
    void insertaYBuscaPorNumero() {
        repositorio.saveAndFlush(Datos.reclamo(2026, 1));
        em.clear();

        Reclamo r = repositorio.findByNumero("RC-2026-000001").orElseThrow();
        assertThat(r.getEstado()).isEqualTo(EstadoReclamo.REGISTRADO);
        assertThat(r.getConsumidor().getNombres()).isEqualTo("María");
        assertThat(r.getConsumidor().getEmail()).isEqualTo("maria@correo.pe");
        assertThat(r.getBien().getMontoReclamado()).isEqualByComparingTo("89.90");
        assertThat(r.getBien().getPedidoCodigo()).isEqualTo("FS-000123");
        assertThat(r.getFechaLimite()).isEqualTo(LocalDate.of(2026, 3, 20));
        assertThat(repositorio.findByNumero("RC-2026-000099")).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 actualiza el estado y registra la respuesta")
    void actualizaEstadoYRespuesta() {
        Reclamo r = repositorio.saveAndFlush(Datos.reclamo(2026, 1));

        r.setEstado(EstadoReclamo.EN_ATENCION);
        repositorio.saveAndFlush(r);
        r.setEstado(EstadoReclamo.RESPONDIDO);
        r.setRespuesta("Procedemos con el cambio.");
        r.setRespondidoEn(Instant.now());
        r.setRespondidoPor("admin-1");
        repositorio.saveAndFlush(r);
        em.clear();

        Reclamo leido = repositorio.findById(r.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoReclamo.RESPONDIDO);
        assertThat(leido.getRespuesta()).isEqualTo("Procedemos con el cambio.");
        assertThat(leido.getRespondidoPor()).isEqualTo("admin-1");
    }

    @Test
    @DisplayName("CA-0a-06 D8: los reclamos nunca se eliminan; el repositorio no expone borrado")
    void noExponeBorrado() {
        assertThat(Arrays.stream(ReclamoRepository.class.getMethods()).map(Method::getName))
                .noneMatch(nombre -> nombre.startsWith("delete") || nombre.startsWith("remove"));
    }

    @Test
    @DisplayName("CA-0a-06 el número de reclamo es único")
    void numeroUnico() {
        repositorio.saveAndFlush(Datos.reclamo(2026, 1));
        Reclamo repetido = Datos.reclamo(2026, 2);
        repetido.setNumero("RC-2026-000001");

        assertThatThrownBy(() -> repositorio.saveAndFlush(repetido)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 (año, correlativo) es único")
    void anioYCorrelativoUnicos() {
        repositorio.saveAndFlush(Datos.reclamo(2026, 1));
        Reclamo repetido = Datos.reclamo(2026, 1);
        repetido.setNumero("RC-2026-999999");

        assertThatThrownBy(() -> repositorio.saveAndFlush(repetido)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 el mismo correlativo puede repetirse en otro año")
    void mismoCorrelativoEnOtroAnio() {
        repositorio.save(Datos.reclamo(2026, 1));
        repositorio.save(Datos.reclamo(2027, 1));
        repositorio.flush();

        assertThat(repositorio.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("CA-0a-06 CHECK: un menor de edad exige el nombre del apoderado")
    void menorSinApoderado() {
        Reclamo r = Datos.reclamo(2026, 1);
        r.getConsumidor().setEsMenor(true);
        r.getConsumidor().setApoderadoNombre(null);

        assertThatThrownBy(() -> repositorio.saveAndFlush(r)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 un menor de edad con apoderado se registra")
    void menorConApoderado() {
        Reclamo r = Datos.reclamo(2026, 2);
        r.getConsumidor().setEsMenor(true);
        r.getConsumidor().setApoderadoNombre("Rosa Huamán");

        assertThat(repositorio.saveAndFlush(r).getId()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 lista por estado y por tipo, del más reciente al más antiguo")
    void listaPorEstadoYTipo() {
        Reclamo a = Datos.reclamo(2026, 1);
        a.setCreadoEn(Instant.parse("2026-02-01T10:00:00Z"));
        Reclamo b = Datos.reclamo(2026, 2);
        b.setCreadoEn(Instant.parse("2026-02-02T10:00:00Z"));
        b.setTipo(TipoReclamo.QUEJA);
        Reclamo c = Datos.reclamo(2026, 3);
        c.setCreadoEn(Instant.parse("2026-02-03T10:00:00Z"));
        c.setEstado(EstadoReclamo.RESPONDIDO);
        repositorio.saveAll(java.util.List.of(a, b, c));
        repositorio.flush();

        assertThat(repositorio.findByEstadoOrderByCreadoEnDesc(EstadoReclamo.REGISTRADO))
                .extracting(Reclamo::getNumero).containsExactly("RC-2026-000002", "RC-2026-000001");
        assertThat(repositorio.findByTipoOrderByCreadoEnDesc(TipoReclamo.QUEJA))
                .extracting(Reclamo::getNumero).containsExactly("RC-2026-000002");
        assertThat(repositorio.findByCreadoEnBetweenOrderByCreadoEnDesc(
                Instant.parse("2026-02-02T00:00:00Z"), Instant.parse("2026-02-03T23:59:59Z")))
                .extracting(Reclamo::getNumero).containsExactly("RC-2026-000003", "RC-2026-000002");
    }

    @Test
    @DisplayName("CA-0a-06 lista los reclamos por vencer y los vencidos (los respondidos no cuentan)")
    void porVencerYVencidos() {
        LocalDate hoy = LocalDate.of(2026, 5, 20);
        Reclamo vencido = conLimite(1, hoy.minusDays(1), EstadoReclamo.REGISTRADO);
        Reclamo venceHoy = conLimite(2, hoy, EstadoReclamo.EN_ATENCION);
        Reclamo porVencer = conLimite(3, hoy.plusDays(3), EstadoReclamo.REGISTRADO);
        Reclamo holgado = conLimite(4, hoy.plusDays(10), EstadoReclamo.REGISTRADO);
        Reclamo respondidoVencido = conLimite(5, hoy.minusDays(5), EstadoReclamo.RESPONDIDO);
        repositorio.saveAll(java.util.List.of(vencido, venceHoy, porVencer, holgado, respondidoVencido));
        repositorio.flush();

        assertThat(repositorio.findVencidos(hoy)).extracting(Reclamo::getCorrelativo).containsExactly(1);
        assertThat(repositorio.findPorVencer(hoy, hoy.plusDays(3)))
                .extracting(Reclamo::getCorrelativo).containsExactly(2, 3);
    }

    @Test
    @DisplayName("CA-0a-06 busca por número, nombre o documento")
    void buscaPorTexto() {
        Reclamo maria = Datos.reclamo(2026, 1);
        Reclamo juan = Datos.reclamo(2026, 2);
        juan.getConsumidor().setNombres("Juan");
        juan.getConsumidor().setApellidos("Pérez");
        juan.getConsumidor().setNumeroDocumento("70112233");
        repositorio.saveAll(java.util.List.of(maria, juan));
        repositorio.flush();

        assertThat(repositorio.buscarPorTexto("%000002%")).extracting(Reclamo::getCorrelativo).containsExactly(2);
        assertThat(repositorio.buscarPorTexto("%quispe%")).extracting(Reclamo::getCorrelativo).containsExactly(1);
        assertThat(repositorio.buscarPorTexto("%juan pérez%")).extracting(Reclamo::getCorrelativo).containsExactly(2);
        assertThat(repositorio.buscarPorTexto("%70112233%")).extracting(Reclamo::getCorrelativo).containsExactly(2);
        assertThat(repositorio.buscarPorTexto("%nadie%")).isEmpty();
    }

    private Reclamo conLimite(int correlativo, LocalDate limite, EstadoReclamo estado) {
        Reclamo r = Datos.reclamo(2026, correlativo);
        r.setFechaLimite(limite);
        r.setEstado(estado);
        return r;
    }
}
