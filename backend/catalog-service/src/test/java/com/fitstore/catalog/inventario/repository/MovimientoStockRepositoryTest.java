package com.fitstore.catalog.inventario.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fitstore.catalog.catalogo.model.Producto;
import com.fitstore.catalog.catalogo.model.Variante;
import com.fitstore.catalog.catalogo.repository.ColorRepository;
import com.fitstore.catalog.catalogo.repository.ProductoRepository;
import com.fitstore.catalog.catalogo.repository.TallaRepository;
import com.fitstore.catalog.catalogo.repository.VarianteRepository;
import com.fitstore.catalog.inventario.model.MovimientoStock;
import com.fitstore.catalog.inventario.model.TipoMovimiento;
import com.fitstore.catalog.pruebas.Datos;
import com.fitstore.catalog.pruebas.PruebaRepositorio;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

@PruebaRepositorio
class MovimientoStockRepositoryTest {

    @Autowired MovimientoStockRepository repositorio;
    @Autowired ProductoRepository productos;
    @Autowired VarianteRepository variantes;
    @Autowired TallaRepository tallas;
    @Autowired ColorRepository colores;
    @Autowired JdbcTemplate jdbc;

    Variante variante;

    @BeforeEach
    void variante() {
        Producto p = productos.saveAndFlush(Datos.producto("MOV-01", "Legging Kardex", "legging-kardex", "59.90"));
        variante = variantes.saveAndFlush(Datos.variante(p.getId(), tallas.findByCodigo("M").orElseThrow().getId(),
                colores.findByNombreIgnoreCase("negro").orElseThrow().getId(), "MOV-01-NEGRO-M"));
    }

    private MovimientoStock movimiento(TipoMovimiento tipo, int cantidad, int saldo, Instant en) {
        MovimientoStock m = new MovimientoStock();
        m.setVarianteId(variante.getId());
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setSaldoFisico(saldo);
        m.setMotivo("Lote de producción");
        m.setCreadoEn(en);
        return m;
    }

    @Test
    @DisplayName("CA-0a-06 MovimientoStockRepository inserta un movimiento de sistema (usuario NULL) con su fecha")
    void inserta() {
        MovimientoStock m = movimiento(TipoMovimiento.ENTRADA, 20, 20, null);
        m.setUsuarioId(null);
        m.setReferencia("OC-15");
        repositorio.saveAndFlush(m);

        MovimientoStock leido = repositorio.findById(m.getId()).orElseThrow();
        assertThat(leido.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(leido.getUsuarioId()).isNull();
        assertThat(leido.getReferencia()).isEqualTo("OC-15");
        assertThat(leido.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("CA-0a-06 MovimientoStockRepository guarda el usuario del staff, la cantidad con signo y el saldo resultante")
    void guardaUsuarioYSigno() {
        UUID usuario = UUID.randomUUID();
        MovimientoStock m = movimiento(TipoMovimiento.AJUSTE, -3, 17, null);
        m.setUsuarioId(usuario);
        repositorio.saveAndFlush(m);

        MovimientoStock leido = repositorio.findById(m.getId()).orElseThrow();
        assertThat(leido.getUsuarioId()).isEqualTo(usuario);
        assertThat(leido.getCantidad()).isEqualTo(-3);
        assertThat(leido.getSaldoFisico()).isEqualTo(17);
    }

    @Test
    @DisplayName("CA-0a-06 MovimientoStockRepository lista el kardex paginado del más reciente al más antiguo")
    void kardexPaginado() {
        Instant base = Instant.parse("2026-10-01T10:00:00Z");
        for (int i = 0; i < 5; i++) {
            repositorio.saveAndFlush(movimiento(TipoMovimiento.ENTRADA, 1, i + 1, base.plusSeconds(i * 60L)));
        }

        Page<MovimientoStock> primera = repositorio.findByVarianteIdOrderByCreadoEnDesc(variante.getId(), PageRequest.of(0, 2));
        Page<MovimientoStock> ultima = repositorio.findByVarianteIdOrderByCreadoEnDesc(variante.getId(), PageRequest.of(2, 2));

        assertThat(primera.getTotalElements()).isEqualTo(5);
        assertThat(primera.getTotalPages()).isEqualTo(3);
        assertThat(primera.getContent()).extracting(MovimientoStock::getSaldoFisico).containsExactly(5, 4);
        assertThat(ultima.getContent()).extracting(MovimientoStock::getSaldoFisico).containsExactly(1);
    }

    @Test
    @DisplayName("CA-0a-06 MovimientoStockRepository solo lista los movimientos de la variante pedida")
    void soloLaVariante() {
        repositorio.saveAndFlush(movimiento(TipoMovimiento.ENTRADA, 5, 5, null));

        assertThat(repositorio.findByVarianteIdOrderByCreadoEnDesc(variante.getId() + 9999, PageRequest.of(0, 10)))
                .isEmpty();
    }

    @Test
    @DisplayName("CA-0a-06 MovimientoStockRepository tiene el índice (variante_id, creado_en DESC) del kardex")
    void indiceDelKardex() {
        List<String> indices = jdbc.queryForList(
                "SELECT indexdef FROM pg_indexes WHERE tablename = 'movimiento_stock'", String.class);

        assertThat(indices).anyMatch(d -> d.contains("variante_id") && d.contains("creado_en DESC"));
    }

    @Test
    @DisplayName("CA-0a-06 MovimientoStockRepository rechaza un movimiento de una variante inexistente")
    void varianteInexistente() {
        MovimientoStock m = movimiento(TipoMovimiento.ENTRADA, 1, 1, null);
        m.setVarianteId(-5L);

        assertThatThrownBy(() -> repositorio.saveAndFlush(m)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
