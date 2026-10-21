package com.fitstore.catalog.inventario.repository;

import com.fitstore.catalog.inventario.dto.EstadoStock;
import com.fitstore.catalog.inventario.dto.StockDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consultas de lectura y cambios atómicos de stock con SQL (el módulo inventario no importa entidades de catálogo: se
 * relaciona con ellas por {@code variante_id}). Cada cambio de {@code fisico} devuelve el saldo resultante en la misma
 * sentencia, así el kardex y la alerta de stock bajo ven el valor exacto aunque haya reservas concurrentes.
 */
@Repository
public class StockConsultas {

    /** Datos de una variante para armar el evento {@code StockBajo}. */
    public record DatosAlerta(String sku, String producto, String talla, String color, int minimo) {
    }

    /** Físico y reservado después de un cambio. */
    public record Saldo(int fisico, int reservado) {
        public int disponible() {
            return fisico - reservado;
        }
    }

    private static final String SELECT_STOCK = """
            SELECT s.variante_id, v.sku, p.id AS producto_id, p.nombre AS producto, t.codigo AS talla, c.nombre AS color,
                   s.fisico, s.reservado, (s.minimo IS NOT NULL) AS minimo_propio,
                   coalesce(s.minimo, ci.stock_minimo_por_defecto) AS minimo
            FROM stock s
            JOIN variante v ON v.id = s.variante_id
            JOIN producto p ON p.id = v.producto_id
            JOIN talla t ON t.id = v.talla_id
            JOIN color c ON c.id = v.color_id
            CROSS JOIN configuracion_inventario ci
            WHERE ci.id = 1
            """;

    private final JdbcClient jdbc;

    public StockConsultas(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<StockDto> buscar(Long varianteId) {
        return jdbc.sql(SELECT_STOCK + " AND s.variante_id = :id").param("id", varianteId)
                .query((rs, n) -> aDto(rs)).optional();
    }

    /** Filtros de P-2-07: texto (SKU o producto), producto, solo stock bajo y solo agotados. */
    public Page<StockDto> listar(String q, Long productoId, boolean soloBajo, boolean soloAgotados, Pageable paginacion) {
        StringBuilder where = new StringBuilder();
        List<Object[]> parametros = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String patron = "%" + q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            where.append(" AND (lower(v.sku) LIKE :patron OR f_unaccent(lower(p.nombre)) LIKE f_unaccent(:patron))");
            parametros.add(new Object[] {"patron", patron});
        }
        if (productoId != null) {
            where.append(" AND p.id = :productoId");
            parametros.add(new Object[] {"productoId", productoId});
        }
        if (soloBajo) {
            where.append(" AND s.fisico - s.reservado <= coalesce(s.minimo, ci.stock_minimo_por_defecto)");
        }
        if (soloAgotados) {
            where.append(" AND s.fisico - s.reservado <= 0");
        }
        var conteo = jdbc.sql("SELECT count(*) FROM (" + SELECT_STOCK + where + ") x");
        var consulta = jdbc.sql(SELECT_STOCK + where + " ORDER BY " + ordenSql(paginacion.getSort())
                + " LIMIT :limite OFFSET :desde");
        for (Object[] par : parametros) {
            conteo = conteo.param((String) par[0], par[1]);
            consulta = consulta.param((String) par[0], par[1]);
        }
        long total = conteo.query(Long.class).single();
        List<StockDto> filas = consulta.param("limite", paginacion.getPageSize()).param("desde", paginacion.getOffset())
                .query((rs, n) -> aDto(rs)).list();
        return new PageImpl<>(filas, paginacion, total);
    }

    /** Las propiedades del {@link Sort} ya son expresiones SQL de una lista blanca (ver {@code InventarioServiceImpl}). */
    private static String ordenSql(Sort sort) {
        StringBuilder orden = new StringBuilder();
        for (Sort.Order o : sort) {
            orden.append(o.getProperty()).append(o.isAscending() ? " ASC, " : " DESC, ");
        }
        return orden.append("v.id ASC").toString();
    }

    /**
     * Suma {@code delta} al físico de forma atómica y devuelve el saldo. Con {@code respetarReservado} no permite dejar el
     * físico por debajo de lo reservado. Vacío si la variante no tiene stock o la condición no se cumple.
     */
    public Optional<Saldo> aplicarDelta(Long varianteId, int delta, boolean respetarReservado) {
        String condicion = respetarReservado ? " AND fisico + :delta >= reservado" : "";
        return jdbc.sql("UPDATE stock SET fisico = fisico + :delta, actualizado_en = now() WHERE variante_id = :id"
                        + condicion + " RETURNING fisico, reservado")
                .param("delta", delta).param("id", varianteId)
                .query((rs, n) -> new Saldo(rs.getInt("fisico"), rs.getInt("reservado"))).optional();
    }

    public boolean existe(Long varianteId) {
        return jdbc.sql("SELECT count(*) FROM stock WHERE variante_id = :id").param("id", varianteId)
                .query(Integer.class).single() > 0;
    }

    /** @return {@code false} si la variante no tiene stock */
    public boolean cambiarMinimo(Long varianteId, Integer minimo) {
        return jdbc.sql("UPDATE stock SET minimo = :minimo, actualizado_en = now() WHERE variante_id = :id")
                .param("minimo", minimo, java.sql.Types.INTEGER).param("id", varianteId).update() > 0;
    }

    public Optional<DatosAlerta> datosAlerta(Long varianteId) {
        return jdbc.sql("""
                SELECT v.sku, p.nombre AS producto, t.codigo AS talla, c.nombre AS color,
                       coalesce(s.minimo, ci.stock_minimo_por_defecto) AS minimo
                FROM stock s
                JOIN variante v ON v.id = s.variante_id
                JOIN producto p ON p.id = v.producto_id
                JOIN talla t ON t.id = v.talla_id
                JOIN color c ON c.id = v.color_id
                CROSS JOIN configuracion_inventario ci
                WHERE ci.id = 1 AND s.variante_id = :id
                """).param("id", varianteId)
                .query((rs, n) -> new DatosAlerta(rs.getString("sku"), rs.getString("producto"), rs.getString("talla"),
                        rs.getString("color"), rs.getInt("minimo")))
                .optional();
    }

    private static StockDto aDto(java.sql.ResultSet rs) throws java.sql.SQLException {
        int fisico = rs.getInt("fisico");
        int reservado = rs.getInt("reservado");
        int disponible = fisico - reservado;
        int minimo = rs.getInt("minimo");
        EstadoStock estado = disponible <= 0 ? EstadoStock.AGOTADO : disponible <= minimo ? EstadoStock.BAJO : EstadoStock.OK;
        return new StockDto(rs.getLong("variante_id"), rs.getString("sku"), rs.getLong("producto_id"), rs.getString("producto"),
                rs.getString("talla"), rs.getString("color"), fisico, reservado, disponible, minimo,
                rs.getBoolean("minimo_propio"), estado);
    }

    /** Disponible actual de una variante; vacío si no tiene fila de stock. */
    public Optional<Integer> disponible(Long varianteId) {
        return jdbc.sql("SELECT fisico - reservado FROM stock WHERE variante_id = :id").param("id", varianteId)
                .query(Integer.class).optional();
    }

    /**
     * Bloquea las filas de stock (en orden de variante, igual que las reservas, para no cruzar bloqueos) y devuelve su
     * disponible. Las variantes sin fila de stock no aparecen.
     */
    public java.util.Map<Long, Integer> bloquearDisponibles(java.util.Collection<Long> varianteIds) {
        java.util.Map<Long, Integer> resultado = new java.util.LinkedHashMap<>();
        if (varianteIds.isEmpty()) {
            return resultado;
        }
        jdbc.sql("SELECT variante_id, fisico - reservado AS disponible FROM stock WHERE variante_id IN (:ids) "
                        + "ORDER BY variante_id FOR UPDATE")
                .param("ids", varianteIds)
                .query((rs, n) -> {
                    resultado.put(rs.getLong("variante_id"), rs.getInt("disponible"));
                    return n;
                }).list();
        return resultado;
    }

    /** Físico y reservado actuales de una variante. */
    public Optional<Saldo> saldo(Long varianteId) {
        return jdbc.sql("SELECT fisico, reservado FROM stock WHERE variante_id = :id").param("id", varianteId)
                .query((rs, n) -> new Saldo(rs.getInt("fisico"), rs.getInt("reservado"))).optional();
    }
}
