package com.fitstore.catalog.pruebas;

import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Datos para las pruebas que no pueden correr dentro de una transacción (concurrencia y consumidores de eventos): cada
 * fila se confirma al crearla y {@link #limpiar()} la borra al terminar. Usa SQL directo para no depender de las
 * entidades.
 */
public final class DatosConfirmados {

    private final JdbcTemplate jdbc;
    private final List<Long> productoIds = new ArrayList<>();
    private final List<Long> varianteIds = new ArrayList<>();

    public DatosConfirmados(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Un producto ACTIVO con una variante M negro y su stock; devuelve el id de la variante. */
    public long variante(String codigo, int fisico, int reservado) {
        Long productoId = jdbc.queryForObject(
                "INSERT INTO producto (codigo, nombre, slug, precio_base, precio_desde, estado, creado_en, actualizado_en) "
                        + "VALUES (?, ?, ?, 50, 50, 'ACTIVO', now(), now()) RETURNING id",
                Long.class, codigo, "Producto " + codigo, "producto-" + codigo.toLowerCase());
        productoIds.add(productoId);
        Long tallaId = jdbc.queryForObject("SELECT id FROM talla WHERE codigo = 'M'", Long.class);
        Long colorId = jdbc.queryForObject("SELECT id FROM color WHERE lower(nombre) = 'negro'", Long.class);
        Long varianteId = jdbc.queryForObject(
                "INSERT INTO variante (producto_id, talla_id, color_id, sku, creado_en, actualizado_en) "
                        + "VALUES (?, ?, ?, ?, now(), now()) RETURNING id",
                Long.class, productoId, tallaId, colorId, codigo + "-NEG-M");
        varianteIds.add(varianteId);
        jdbc.update("INSERT INTO stock (variante_id, fisico, reservado) VALUES (?, ?, ?)", varianteId, fisico, reservado);
        return varianteId;
    }

    public int fisico(long varianteId) {
        return jdbc.queryForObject("SELECT fisico FROM stock WHERE variante_id = ?", Integer.class, varianteId);
    }

    public int reservado(long varianteId) {
        return jdbc.queryForObject("SELECT reservado FROM stock WHERE variante_id = ?", Integer.class, varianteId);
    }

    public String estadoReserva(Object pedidoId) {
        List<String> estados = jdbc.queryForList("SELECT estado FROM reserva WHERE pedido_id = ?::uuid", String.class,
                pedidoId.toString());
        return estados.isEmpty() ? null : estados.get(0);
    }

    public int movimientos(long varianteId, String tipo) {
        return jdbc.queryForObject("SELECT count(*) FROM movimiento_stock WHERE variante_id = ? AND tipo = ?", Integer.class,
                varianteId, tipo);
    }

    public int eventosPublicados(String tipo) {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_evento WHERE tipo = ?", Integer.class, tipo);
    }

    public void limpiar() {
        jdbc.update("DELETE FROM reserva_item");
        jdbc.update("DELETE FROM reserva");
        jdbc.update("DELETE FROM movimiento_stock");
        jdbc.update("DELETE FROM outbox_evento");
        jdbc.update("DELETE FROM evento_procesado");
        for (Long id : varianteIds) {
            jdbc.update("DELETE FROM stock WHERE variante_id = ?", id);
            jdbc.update("DELETE FROM variante WHERE id = ?", id);
        }
        for (Long id : productoIds) {
            jdbc.update("DELETE FROM producto WHERE id = ?", id);
        }
        varianteIds.clear();
        productoIds.clear();
    }
}
