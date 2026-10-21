"""Genera backend/catalog-service/src/main/resources/db/dev/R__catalogo_demo.sql desde catalogo_demo.py.

Uso (desde la raíz del repositorio):  python3 infra/scripts/semilla-imagenes/generar_sql.py
El SQL es idempotente: cada INSERT lleva ON CONFLICT DO NOTHING, así que se puede aplicar de nuevo (migración repetible).
"""

import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import catalogo_demo as demo  # noqa: E402

DESTINO = "backend/catalog-service/src/main/resources/db/dev/R__catalogo_demo.sql"


def q(texto):
    return "'" + texto.replace("'", "''") + "'"


def main():
    sql = [
        "-- Datos de demostración del catálogo (SPEC-2 sección 6) para los perfiles dev y demo (SPEC-8 I7).",
        "-- GENERADO por infra/scripts/semilla-imagenes/generar_sql.py desde catalogo_demo.py: no se edita a mano.",
        "-- IDs explícitos en los rangos de docs/demo/ids-demo.md; al final se ajusta cada secuencia.",
        "",
        "-- Color maestro adicional para estampados y surtidos",
        "INSERT INTO color (nombre, hex) SELECT 'Variado', '#A855F7'",
        "WHERE NOT EXISTS (SELECT 1 FROM color WHERE lower(nombre) = 'variado');",
        "",
        "-- Categorías (2 niveles)",
        "INSERT INTO categoria (id, padre_id, nombre, slug, orden, activo, creado_en, actualizado_en) VALUES",
    ]
    filas = []
    for cid, padre, nombre, orden, _g in demo.CATEGORIAS:
        filas.append(f"    ({cid}, {padre if padre else 'NULL'}, {q(nombre)}, {q(demo.slug(nombre))}, {orden}, true, now(), now())")
    sql.append(",\n".join(filas) + "\nON CONFLICT (id) DO NOTHING;\n")

    sql.append("-- Guías de tallas de las categorías que la heredan sus hijas")
    for cid, (columnas, tallas) in demo.GUIAS.items():
        tabla = json.dumps({"columnas": columnas, "filas": tallas}, ensure_ascii=False)
        sql.append(f"INSERT INTO guia_tallas (categoria_id, tabla, nota) VALUES ({cid}, {q(tabla)}::jsonb, "
                   f"'Medidas del cuerpo en centímetros') ON CONFLICT (categoria_id) DO NOTHING;")
    sql.append("")

    sql.append("-- Productos")
    sql.append("INSERT INTO producto (id, codigo, nombre, slug, descripcion, precio_base, precio_desde, estado, destacado, "
               "orden_destacado, modo_descuento, creado_en, actualizado_en, version) VALUES")
    filas = []
    for pid, codigo, nombre, descripcion, precio, estado, destacado, _cats, _forma, _col, _tal, modo in demo.PRODUCTOS:
        filas.append(f"    ({pid}, {q(codigo)}, {q(nombre)}, {q(demo.slug(nombre))}, {q(descripcion)}, {precio}, {precio}, "
                     f"{q(estado)}, {'true' if destacado else 'false'}, {destacado if destacado else 'NULL'}, {q(modo)}, now() - interval '{len(demo.PRODUCTOS) - (pid - 1000)} days', now(), 0)")
    sql.append(",\n".join(filas) + "\nON CONFLICT (id) DO NOTHING;\n")

    sql.append("INSERT INTO producto_categoria (producto_id, categoria_id) VALUES")
    pares = [f"    ({p[0]}, {c})" for p in demo.PRODUCTOS for c in p[7]]
    sql.append(",\n".join(pares) + "\nON CONFLICT DO NOTHING;\n")

    if demo.TRAMOS_PROPIOS:
        sql.append("-- Tramos propios de los productos en modo PERSONALIZADO")
        sql.append("INSERT INTO tramo_descuento (producto_id, unidades_minimas, porcentaje) VALUES")
        sql.append(",\n".join(f"    ({pid}, {u}, {p})" for pid, tramos in demo.TRAMOS_PROPIOS.items() for u, p in tramos)
                   + "\nON CONFLICT DO NOTHING;\n")

    vs = demo.variantes()
    sql.append("-- Variantes (talla y color por nombre, para no depender de los ids de los maestros)")
    sql.append("INSERT INTO variante (id, producto_id, talla_id, color_id, sku, precio, activo, creado_en, actualizado_en)")
    sql.append("SELECT v.id, v.producto_id, t.id, c.id, v.sku, NULL, true, now(), now() FROM (VALUES")
    sql.append(",\n".join(f"    ({i}, {p}, {q(t)}, {q(c.lower())}, {q(sku)})" for i, p, t, c, sku, _f, _r in vs))
    sql.append(") AS v(id, producto_id, talla, color, sku)")
    sql.append("JOIN talla t ON t.codigo = v.talla JOIN color c ON lower(c.nombre) = v.color")
    sql.append("ON CONFLICT (id) DO NOTHING;\n")

    sql.append("-- Stock variado: agotados, últimas unidades y stock sano")
    sql.append("INSERT INTO stock (variante_id, fisico, reservado, actualizado_en) VALUES")
    sql.append(",\n".join(f"    ({i}, {f}, {r}, now())" for i, _p, _t, _c, _s, f, r in vs) + "\nON CONFLICT (variante_id) DO NOTHING;\n")

    sql.append("-- Kardex: una entrada inicial por cada variante con stock (movimiento del sistema)")
    sql.append("INSERT INTO movimiento_stock (variante_id, tipo, cantidad, saldo_fisico, motivo, usuario_id, referencia, creado_en)")
    sql.append("SELECT s.variante_id, 'ENTRADA', s.fisico, s.fisico, 'Stock inicial de demostración', NULL, NULL, now() - interval '7 days'")
    sql.append("FROM stock s WHERE s.variante_id BETWEEN 10001 AND 19999 AND s.fisico > 0")
    sql.append("AND NOT EXISTS (SELECT 1 FROM movimiento_stock m WHERE m.variante_id = s.variante_id);\n")

    sql.append("-- Imágenes: una por producto y color; los archivos los sube catalog a Garage al arrancar (dev o demo)")
    sql.append("INSERT INTO imagen (id, producto_id, color_id, clave, orden, principal, ancho, alto)")
    sql.append("SELECT i.id, i.producto_id, c.id, i.clave, i.orden, i.principal, 1200, 1200 FROM (VALUES")
    sql.append(",\n".join(f"    ({i}, {p}, {q(c.lower())}, {q(clave)}, {o}, {'true' if pr else 'false'})" for i, p, c, clave, pr, o in demo.imagenes()))
    sql.append(") AS i(id, producto_id, color, clave, orden, principal)")
    sql.append("JOIN color c ON lower(c.nombre) = i.color")
    sql.append("ON CONFLICT (id) DO NOTHING;\n")

    sql.append("-- Cada secuencia queda después del mayor id de la demo, para que lo que cree el admin no choque")
    for tabla in ("categoria", "producto", "variante", "imagen"):
        sql.append(f"SELECT setval(pg_get_serial_sequence('{tabla}', 'id'), (SELECT max(id) FROM {tabla}));")
    sql.append("")
    with open(DESTINO, "w", encoding="utf-8") as f:
        f.write("\n".join(sql))
    print(f"{DESTINO}: {len(vs)} variantes, {len(demo.imagenes())} imágenes")


if __name__ == "__main__":
    main()
