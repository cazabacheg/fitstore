# Extensión del alcance

> Lead · 2026-10-20 · Decisión del autor: para llegar a tiempo con el flujo completo de la tienda (carrito → checkout → pago → pedido → entrega → correos), estas historias y pantallas **salen del alcance de implementación actual** y quedan como **extensión**: están especificadas y aprobadas, y se pueden construir después sin rediseñar nada.
> Los criterios de esta lista **no cuentan** para el "terminado" de su spec (V0b) ni para `scripts/ca-cobertura.sh`, que los excluye leyendo este archivo. Ningún brief los asigna.

## Qué queda fuera

| Spec | Historia o pantalla | Criterios | Qué implica dejarla fuera |
|---|---|---|---|
| 2 | P-2-09 Carga masiva (solo la pantalla) | — | El backend de la carga masiva (CA-2-58 a CA-2-61) ya estaba implementado y se conserva: se usa por API. La ruta `/catalogo/productos/carga-masiva` y su ítem del menú muestran "Próximamente" |
| 4 | HU-4-02 Conservar mi carrito al iniciar sesión | CA-4-09, CA-4-10, CA-4-11, CA-4-12 | El carrito anónimo y el del cliente son independientes; al ir al checkout sin sesión, el login lleva de vuelta con `returnUrl` |
| 4 | HU-4-11 Datos de ventas para el dashboard | CA-4-51, CA-4-52, CA-4-53 | Sin `GET /admin/pedidos/resumen`; el dashboard de la SPEC-8 se revisa al preparar su brief |
| 4 | HU-4-12 Reembolsar un pedido entregado o devuelto | CA-4-54, CA-4-55, CA-4-56, CA-4-57, CA-4-58, CA-4-59 | Sin reembolso postventa ni el evento `ReembolsoSolicitado` |
| 5 | HU-5-03 Pagar con tarjeta (Culqi) | CA-5-09, CA-5-10, CA-5-11, CA-5-12 | Queda una sola pasarela real: **Mercado Pago** (sandbox), que también acepta tarjeta, más el proveedor simulado de la demo. Settings conserva las opciones Culqi y Yape en sus parámetros, pero la demo solo habilita Mercado Pago |
| 5 | HU-5-04 Pagar con Yape | CA-5-13, CA-5-14 | Igual que la anterior |
| 5 | HU-5-10 Devolver el dinero automáticamente | CA-5-30, CA-5-31, CA-5-32, CA-5-39, CA-5-40 | Cancelar un pedido pagado queda registrado (`PedidoCancelado` con `fuePagado`), pero la devolución del dinero se hace fuera del sistema |
| 5 | HU-5-12 Atender reembolsos fallidos · P-5-05 Reembolsos | CA-5-36, CA-5-37, CA-5-38 | La ruta `/reembolsos` y su ítem del menú muestran "Próximamente"; sin contador de reembolsos fallidos en el menú |
| 6 | HU-6-08 Planificar el día y la hoja de ruta · P-6-03 | CA-6-28, CA-6-29, CA-6-30 | La ruta `/entregas/dia` y su ítem del menú muestran "Próximamente" |
| 6 | HU-6-10 Devolver a la tienda una entrega que no se concretó | CA-6-33, CA-6-34 | Una entrega FALLIDA o no recogida queda en ese estado |
| 7 | HU-7-06 Editar los textos de los correos · P-7-02, P-7-03 | CA-7-23, CA-7-24, CA-7-25, CA-7-26, CA-7-27, CA-7-30 | Los correos usan los textos predeterminados de las plantillas. La ruta `/notificaciones/plantillas` muestra "Próximamente" |
| 7 | HU-7-07 No guardar datos personales más de lo necesario | CA-7-28 | Sin la limpieza diaria de retención |
| 8 | HU-8-02 (parte) Prueba E2E de postventa y devolución | CA-8-08 | Depende de los reembolsos (HU-4-12 y HU-5-10), que ya son extensión |
| 8 | HU-8-01 (parte) Ventas y top de productos del dashboard | — | Dependen de `GET /admin/pedidos/resumen` (HU-4-11). El dashboard muestra pedidos por estado, stock bajo, entregas por estado y reclamos por vencer |

## Desvíos acordados al desplegar (SPEC-8)

| Tema | Lo que dice la spec | Lo que se hace en esta entrega | Por qué |
|---|---|---|---|
| Correo en la nube | Amazon SES con remitente verificado (CA-8-24) | Mailpit en la VM, sin publicar, visto por túnel SSH (el plan B que ya prevé el guion de demo) | SES exige otra cuenta (AWS) con tarjeta; para la demo los correos se ven igual |
| IP pública y DuckDNS | IP dinámica y contenedor que actualiza DuckDNS cada 5 min (CA-8-19) | IP pública estática de Azure; el autor la registra una vez en DuckDNS | La IP no cambia al apagar la VM y no hace falta guardar el token de DuckDNS |
| Tamaño de la VM | `B2ms` (2 vCPU, 8 GB) | `B2as_v2` (2 vCPU, 8 GB) en Chile Central | La suscripción de estudiante solo permite cinco regiones; en Chile Central, la más cercana, solo existe la serie Bv2, y es más barata |

## Deuda menor anotada durante la implementación

No son historias nuevas: son detalles que se dejaron simples para llegar a tiempo.

| Spec | Detalle | Estado actual |
|---|---|---|
| 1 | Invitación de staff de punta a punta (P-1-04) con el correo real | Se probó por API; falta recorrerla en la demo con el correo en Mailpit |
| 2 | El kardex muestra el UUID de quien hizo el movimiento | Debería mostrar su nombre |
| 2 | El slug del producto no se edita | El contrato no lo incluye en la edición |
| 2 | La tarjeta de producto no dice "Desde" cuando las variantes tienen precios distintos | Muestra el precio base |
| 3 | Políticas y ubigeo sin caché en el servidor | Se leen de la base en cada petición |
| 5 | La configuración de la tienda se guarda en memoria en payment (5 min), no en Redis | Funciona igual; el micro dice Redis |
| 6 | Servicios de delivery como clases concretas, sin interfaz e `Impl` | ADR-015 pide la interfaz |
| 0b | En varias specs las pruebas del frontend se escribieron junto con la pantalla, no antes | El backend sí tiene sus commits en rojo |

## Orden de prioridad para después del MVP

Si hay tiempo para extender el alcance, se retoma en este orden (primero lo que más se nota en la demo y cuesta menos):

1. **HU-4-02** Conservar el carrito al iniciar sesión: corta, y el usuario lo nota en la primera compra.
2. **HU-5-04 Yape** y **HU-5-03 Culqi**: Yape es el medio de pago más usado por los clientes de Gamarra.
3. **HU-7-06** Editar los textos de los correos (P-7-02 y P-7-03): completa el white-label.
4. **HU-4-11** Datos de ventas y el dashboard completo de la SPEC-8.
5. **Reembolsos** como un solo bloque: HU-5-10, HU-5-12 con P-5-05 y HU-4-12 (dependen entre sí).
6. **HU-6-08** Planificar el día y la hoja de ruta (P-6-03).
7. **HU-6-10** Devolver a la tienda una entrega no concretada (después de los reembolsos).
8. **P-2-09** Pantalla de carga masiva (el backend ya existe).
9. **HU-7-07** Limpieza de retención de datos.
10. **Nube real:** SES con dominio verificado y el contenedor de DuckDNS, si la tienda pasa a producción.
11. La deuda menor de la tabla anterior, cuando se toque cada módulo.

## Desvíos acordados al desplegar (SPEC-8)

| Tema | Lo que dice la spec | Lo que se hace en esta entrega | Por qué |
|---|---|---|---|
| Correo en la nube | Amazon SES con remitente verificado (CA-8-24) | Mailpit en la VM, sin publicar, visto por túnel SSH (el plan B que ya prevé el guion de demo) | SES exige otra cuenta (AWS) con tarjeta; para la demo los correos se ven igual |
| IP pública y DuckDNS | IP dinámica y contenedor que actualiza DuckDNS cada 5 min (CA-8-19) | IP pública estática de Azure; el autor la registra una vez en DuckDNS | La IP no cambia al apagar la VM y no hace falta guardar el token de DuckDNS |
| Tamaño de la VM | `B2ms` (2 vCPU, 8 GB) | `B2as_v2` (2 vCPU, 8 GB) en Chile Central | La suscripción de estudiante solo permite cinco regiones; en Chile Central, la más cercana, solo existe la serie Bv2, y es más barata |

## Deuda menor anotada durante la implementación

No son historias nuevas: son detalles que se dejaron simples para llegar a tiempo.

| Spec | Detalle | Estado actual |
|---|---|---|
| 1 | Invitación de staff de punta a punta (P-1-04) con el correo real | Se probó por API; falta recorrerla en la demo con el correo en Mailpit |
| 2 | El kardex muestra el UUID de quien hizo el movimiento | Debería mostrar su nombre |
| 2 | El slug del producto no se edita | El contrato no lo incluye en la edición |
| 2 | La tarjeta de producto no dice "Desde" cuando las variantes tienen precios distintos | Muestra el precio base |
| 3 | Políticas y ubigeo sin caché en el servidor | Se leen de la base en cada petición |
| 5 | La configuración de la tienda se guarda en memoria en payment (5 min), no en Redis | Funciona igual; el micro dice Redis |
| 6 | Servicios de delivery como clases concretas, sin interfaz e `Impl` | ADR-015 pide la interfaz |
| 0b | En varias specs las pruebas del frontend se escribieron junto con la pantalla, no antes | El backend sí tiene sus commits en rojo |

## Orden de prioridad para después del MVP

Si hay tiempo para extender el alcance, se retoma en este orden (primero lo que más se nota en la demo y cuesta menos):

1. **HU-4-02** Conservar el carrito al iniciar sesión: corta, y el usuario lo nota en la primera compra.
2. **HU-5-04 Yape** y **HU-5-03 Culqi**: Yape es el medio de pago más usado por los clientes de Gamarra.
3. **HU-7-06** Editar los textos de los correos (P-7-02 y P-7-03): completa el white-label.
4. **HU-4-11** Datos de ventas y el dashboard completo de la SPEC-8.
5. **Reembolsos** como un solo bloque: HU-5-10, HU-5-12 con P-5-05 y HU-4-12 (dependen entre sí).
6. **HU-6-08** Planificar el día y la hoja de ruta (P-6-03).
7. **HU-6-10** Devolver a la tienda una entrega no concretada (después de los reembolsos).
8. **P-2-09** Pantalla de carga masiva (el backend ya existe).
9. **HU-7-07** Limpieza de retención de datos.
10. **Nube real:** SES con dominio verificado y el contenedor de DuckDNS, si la tienda pasa a producción.
11. La deuda menor de la tabla anterior, cuando se toque cada módulo.

## Cómo se retoma

Cada historia mantiene su texto, sus criterios y sus pantallas en la spec, marcados como **extensión de alcance**. Para retomarla basta un brief nuevo con esa historia (B2 contra la sección 7 de su spec) y quitarla de esta tabla.
