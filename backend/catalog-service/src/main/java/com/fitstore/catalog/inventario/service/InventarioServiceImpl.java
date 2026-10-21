package com.fitstore.catalog.inventario.service;

import com.fitstore.catalog.catalogo.service.Paginacion;
import com.fitstore.catalog.inventario.dto.AjusteRequest;
import com.fitstore.catalog.inventario.dto.ConfiguracionInventarioDto;
import com.fitstore.catalog.inventario.dto.EntradaRequest;
import com.fitstore.catalog.inventario.dto.KardexDto;
import com.fitstore.catalog.inventario.dto.LineaEntradaRequest;
import com.fitstore.catalog.inventario.dto.MovimientoDto;
import com.fitstore.catalog.inventario.dto.StockDto;
import com.fitstore.catalog.inventario.model.ConfiguracionInventario;
import com.fitstore.catalog.inventario.model.MovimientoStock;
import com.fitstore.catalog.inventario.model.TipoMovimiento;
import com.fitstore.catalog.inventario.repository.ConfiguracionInventarioRepository;
import com.fitstore.catalog.inventario.repository.MovimientoStockRepository;
import com.fitstore.catalog.inventario.repository.StockConsultas;
import com.fitstore.catalog.inventario.repository.StockConsultas.Saldo;
import com.fitstore.common.api.ConflictoNegocioException;
import com.fitstore.common.api.FitstoreException;
import com.fitstore.common.api.PaginaResponse;
import com.fitstore.common.api.RecursoNoEncontradoException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventarioServiceImpl implements InventarioService {

    /** Campo del API → expresión SQL (lista blanca: nunca se arma SQL con texto del cliente). */
    private static final Map<String, String> ORDENES = Map.of(
            "sku", "v.sku", "producto", "p.nombre", "disponible", "(s.fisico - s.reservado)", "fisico", "s.fisico",
            "reservado", "s.reservado", "minimo", "coalesce(s.minimo, ci.stock_minimo_por_defecto)");
    private static final Sort ORDEN_POR_DEFECTO = Sort.by("p.nombre", "v.sku");

    private final StockConsultas consultas;
    private final MovimientoStockRepository movimientos;
    private final ConfiguracionInventarioRepository configuracion;
    private final AlertaStockBajo alerta;

    public InventarioServiceImpl(StockConsultas consultas, MovimientoStockRepository movimientos,
            ConfiguracionInventarioRepository configuracion, AlertaStockBajo alerta) {
        this.consultas = consultas;
        this.movimientos = movimientos;
        this.configuracion = configuracion;
        this.alerta = alerta;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<StockDto> listar(String q, Long productoId, boolean soloStockBajo, boolean soloAgotados, int pagina,
            int tamano, List<String> orden) {
        Pageable paginacion = Paginacion.de(pagina, tamano, orden, ORDENES, ORDEN_POR_DEFECTO);
        Page<StockDto> resultado = consultas.listar(q, productoId, soloStockBajo, soloAgotados, paginacion);
        return PaginaResponse.de(new PageImpl<>(resultado.getContent(), paginacion, resultado.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public KardexDto kardex(Long varianteId, Pageable paginacion) {
        StockDto stock = consultas.buscar(varianteId).orElseThrow(() -> noExiste(varianteId));
        Page<MovimientoDto> pagina = movimientos.findByVarianteIdOrderByCreadoEnDescIdDesc(varianteId, paginacion).map(m ->
                new MovimientoDto(m.getId(), m.getTipo(), m.getCantidad(), m.getSaldoFisico(), m.getMotivo(), m.getReferencia(),
                        m.getUsuarioId() == null ? null : m.getUsuarioId().toString(), m.getCreadoEn()));
        return new KardexDto(stock, PaginaResponse.de(pagina));
    }

    @Override
    public List<StockDto> registrarEntrada(EntradaRequest peticion, UUID usuarioId) {
        // se procesa en orden de variante (el mismo orden con que reservan los pedidos) para evitar bloqueos cruzados
        List<LineaEntradaRequest> ordenadas = new ArrayList<>(peticion.lineas());
        ordenadas.sort(Comparator.comparing(LineaEntradaRequest::varianteId));
        // todo o nada: se comprueba que existan todas las variantes antes de tocar el stock
        for (LineaEntradaRequest linea : ordenadas) {
            if (!consultas.existe(linea.varianteId())) {
                throw noExiste(linea.varianteId());
            }
        }
        String motivo = peticion.motivo() == null || peticion.motivo().isBlank() ? null : peticion.motivo().trim();
        for (LineaEntradaRequest linea : ordenadas) {
            Saldo saldo = consultas.aplicarDelta(linea.varianteId(), linea.cantidad(), false)
                    .orElseThrow(() -> noExiste(linea.varianteId()));
            guardarMovimiento(linea.varianteId(), TipoMovimiento.ENTRADA, linea.cantidad(), saldo.fisico(), motivo, null, usuarioId);
        }
        return peticion.lineas().stream().map(l -> consultas.buscar(l.varianteId()).orElseThrow()).distinct().toList();
    }

    @Override
    public StockDto registrarAjuste(AjusteRequest peticion, UUID usuarioId) {
        if (peticion.cantidad() == 0) {
            throw new FitstoreException(HttpStatus.BAD_REQUEST, "VALIDACION", "El ajuste no puede ser de 0 unidades");
        }
        Saldo saldo = consultas.aplicarDelta(peticion.varianteId(), peticion.cantidad(), true).orElse(null);
        if (saldo == null) {
            if (!consultas.existe(peticion.varianteId())) {
                throw noExiste(peticion.varianteId());
            }
            throw new ConflictoNegocioException("AJUSTE_BAJO_RESERVADO",
                    "El ajuste dejaría el stock físico por debajo de lo reservado por pedidos");
        }
        guardarMovimiento(peticion.varianteId(), TipoMovimiento.AJUSTE, peticion.cantidad(), saldo.fisico(),
                peticion.motivo().trim(), null, usuarioId);
        alerta.evaluar(peticion.varianteId(), saldo.disponible(), peticion.cantidad());
        return consultas.buscar(peticion.varianteId()).orElseThrow();
    }

    private void guardarMovimiento(Long varianteId, TipoMovimiento tipo, int cantidad, int saldoFisico, String motivo,
            String referencia, UUID usuarioId) {
        MovimientoStock m = new MovimientoStock();
        m.setVarianteId(varianteId);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setSaldoFisico(saldoFisico);
        m.setMotivo(motivo);
        m.setReferencia(referencia);
        m.setUsuarioId(usuarioId);
        movimientos.saveAndFlush(m);
    }

    @Override
    public StockDto cambiarMinimo(Long varianteId, Integer minimo) {
        if (!consultas.cambiarMinimo(varianteId, minimo)) {
            throw noExiste(varianteId);
        }
        return consultas.buscar(varianteId).orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionInventarioDto obtenerConfiguracion() {
        ConfiguracionInventario c = configuracion.findById(1L).orElseThrow();
        return new ConfiguracionInventarioDto(c.getStockMinimoPorDefecto(), c.getMargenReservaMinutos());
    }

    @Override
    public ConfiguracionInventarioDto guardarConfiguracion(ConfiguracionInventarioDto dto) {
        ConfiguracionInventario c = configuracion.findById(1L).orElseThrow();
        c.setStockMinimoPorDefecto(dto.stockMinimoPorDefecto());
        c.setMargenReservaMinutos(dto.margenReservaMinutos());
        configuracion.saveAndFlush(c);
        return dto;
    }

    private static RecursoNoEncontradoException noExiste(Long varianteId) {
        return new RecursoNoEncontradoException("No encontramos la variante " + varianteId);
    }
}
