# IDs fijos de los datos de demostración

> Lead · 2026-10-20 · Lo usan las migraciones repetibles `R__*_demo.sql` (`db/dev/`) de cada servicio, con los perfiles `dev` y `demo` (SPEC-8 I7).
> Un mismo dato de demo debe tener **el mismo id en todos los servicios** (un cliente de auth es el dueño de un pedido de order, que cobra payment y entrega delivery).
> Lo amplía el lead con lo que reporta cada spec al terminar; los agentes no editan este archivo.

## auth (SPEC-1)

| Cuenta | Tipo | UUID | Direcciones |
|---|---|---|---|
| Cliente demo 1 | CLIENTE | `35a11d4f-054a-44a5-8ac4-cf0ed4020f8c` | Lima |
| Cliente demo 2 | CLIENTE | `41f4b1c9-00e0-4816-91b9-a1a143a69df9` | Lima y Callao |
| Cliente demo 3 | CLIENTE | `a3d50456-6d33-4e6e-80fc-62563fc46808` | Callao |
| Cliente demo 4 | CLIENTE | `652dcca9-9c40-434a-9d2d-33e44d1dcc16` | Arequipa |
| Operador demo | STAFF · OPERADOR | `5e738102-a490-469f-b0f6-5f1fc2dbf70c` | — |

Nombres, correos, DNI y direcciones: los define la SPEC-1 y se anotan aquí al integrarla.

## catalog (SPEC-2)

Los ids de catalog son `bigint` con identidad. La demo los inserta **explícitos** en estos rangos y al final ajusta cada secuencia (`SELECT setval(pg_get_serial_sequence('producto', 'id'), (SELECT max(id) FROM producto))`), para que lo que cree el admin después no choque.

| Tabla | Rango |
|---|---|
| `categoria` | 101–199 |
| `producto` | 1001–1099 |
| `variante` | 10001–19999 |
| `imagen` | 20001–29999 |

Los SKU de las variantes que usan los pedidos de demostración (SPEC-4) se anotan aquí al integrar la SPEC-2.

## settings (SPEC-3)

Los reclamos de demostración usan números del año en curso con prefijo `RC-`; se anotan aquí al integrar la SPEC-3.
