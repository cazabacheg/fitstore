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

| Cuenta | Nombre | Correo |
|---|---|---|
| Cliente demo 1 | Lucía Mendoza | `lucia.mendoza@fitstore.demo` |
| Cliente demo 2 | Carlos Huamán | `carlos.huaman@fitstore.demo` |
| Cliente demo 3 | Rosa Ccori | `rosa.ccori@fitstore.demo` |
| Cliente demo 4 | Miguel Paredes | `miguel.paredes@fitstore.demo` |
| Operador demo | Diana Flores | `diana.flores@fitstore.demo` |

Contraseña común de **desarrollo**: `FitStoreDemo2026` (en la nube sale del secreto `DEMO_PASSWORD`, CA-8-31). El ADMIN inicial no es de la demo: lo crea auth con `FITSTORE_ADMIN_EMAIL` y `FITSTORE_ADMIN_PASSWORD` del `.env`.

## catalog (SPEC-2)

Los ids de catalog son `bigint` con identidad. La demo los inserta **explícitos** en estos rangos y al final ajusta cada secuencia (`SELECT setval(pg_get_serial_sequence('producto', 'id'), (SELECT max(id) FROM producto))`), para que lo que cree el admin después no choque.

| Tabla | Rango |
|---|---|
| `categoria` | 101–199 |
| `producto` | 1001–1099 |
| `variante` | 10001–19999 |
| `imagen` | 20001–29999 |

Variantes que usan los pedidos de demostración (SPEC-4):

| Variante | SKU |
|---|---|
| 10002 | `LEG01-NEG-M` (Legging Pro Compresión, M, negro) |
| 10014 | `LEG02-NEG-S` |
| 10037 | `JOG01-NEG-L` |
| 10052 | `SHO01-NEG-M` |

## settings (SPEC-3)

Los reclamos de demostración usan números del año en curso con prefijo `RC-`; se anotan aquí al integrar la SPEC-3.

## order (SPEC-4)

| Pedido | UUID | Cliente | Entrega | Estado |
|---|---|---|---|---|
| FS-000101 | `3e13f37f-db57-44c2-8ac1-66d7ce378e0b` | Lucía | local | ENVIADO |
| FS-000102 | `33900490-eace-44ab-a6b3-391a28202b57` | Carlos | local | ENTREGADO |
| FS-000103 | `5928f8f8-ed74-413e-91f4-d3a6115f6afc` | Miguel (Arequipa) | agencia | ENVIADO |
| FS-000104 | `b7655951-8c1f-4f51-9a90-39534aa106d9` | Rosa (Callao) | local | LISTO_DESPACHO |
| FS-000105 a 111 | `a0000000-0000-4000-8000-000000000105` a `…111` | varios | varios | pendiente, pagado, en preparación, listo para recoger, entregado con reembolso parcial, cancelado tras el pago y expirado |

La secuencia de códigos queda en 111: el primer pedido nuevo es FS-000112. Reembolso demo: `b0000000-0000-4000-8000-000000000109`.

## delivery (SPEC-6)

- Entregas de FS-000101 (EN_RUTA), FS-000102 (ENTREGADA), FS-000103 (ENVIADO_AGENCIA por Shalom, código `SH-778899`, clave `4821`) y FS-000104 (PENDIENTE_ASIGNACION).
- Repartidores 101 Juan Pérez Soto (moto), 102 Rosa Quispe Ramos (bicicleta) y 103 Luis Castro Vega (auto, inactivo).
- Agencias: las 4 de la semilla `V2`.

## notification (SPEC-7)

24 envíos de los pedidos demo: uno FALLIDO (el reembolso de FS-000110) y uno PENDIENTE que solo sale al reenviarlo.
