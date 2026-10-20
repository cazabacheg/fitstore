package com.fitstore.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.payment.model.Boleta;
import com.fitstore.payment.model.IntencionPago;
import com.fitstore.payment.pruebas.Datos;
import com.fitstore.payment.pruebas.PruebaRepositorio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@PruebaRepositorio
class BoletaRepositoryTest {

    @Autowired BoletaRepository repositorio;
    @Autowired CorrelativoBoletaRepository correlativos;
    @Autowired IntencionPagoRepository intenciones;
    @Autowired EntityManager em;

    private IntencionPago intencion() {
        return intenciones.saveAndFlush(Datos.intencion());
    }

    @Test
    @DisplayName("CA-0a-06 inserta una boleta y la busca por intención, por pedido y por número")
    void insertaYBusca() {
        IntencionPago intencion = intencion();
        Boleta guardada = repositorio.saveAndFlush(Datos.boleta(intencion.getId(), 1));
        em.clear();

        assertThat(repositorio.findByIntencionId(intencion.getId())).get().extracting(Boleta::getNumero)
                .isEqualTo("B001-00000001");
        assertThat(repositorio.findByPedidoId(intencion.getPedidoId())).get().extracting(Boleta::getId)
                .isEqualTo(guardada.getId());
        assertThat(repositorio.findByNumero("B001-00000001")).isPresent();
        assertThat(repositorio.findByNumero("B001-99999999")).isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 rechaza una segunda boleta para la misma intención (intencion_id único)")
    void rechazaBoletaDuplicadaPorIntencion() {
        IntencionPago intencion = intencion();
        repositorio.saveAndFlush(Datos.boleta(intencion.getId(), 1));

        assertThatThrownBy(() -> repositorio.saveAndFlush(Datos.boleta(intencion.getId(), 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 rechaza repetir (serie, correlativo) en otra intención")
    void rechazaCorrelativoRepetido() {
        repositorio.saveAndFlush(Datos.boleta(intencion().getId(), 1));
        Boleta repetida = Datos.boleta(intencion().getId(), 1);

        assertThatThrownBy(() -> repositorio.saveAndFlush(repetida)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("CA-0a-06 las boletas emitidas con el correlativo atómico quedan numeradas sin huecos")
    void numeracionSinHuecos() {
        int primero = correlativos.siguiente("B001");
        Boleta uno = Datos.boleta(intencion().getId(), primero);
        int segundo = correlativos.siguiente("B001");
        Boleta dos = Datos.boleta(intencion().getId(), segundo);
        repositorio.save(uno);
        repositorio.saveAndFlush(dos);

        assertThat(segundo).isEqualTo(primero + 1);
        assertThat(repositorio.findByNumero(dos.getNumero())).isPresent();
    }
}
