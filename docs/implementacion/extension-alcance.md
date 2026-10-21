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

## Cómo se retoma

Cada historia mantiene su texto, sus criterios y sus pantallas en la spec, marcados como **extensión de alcance**. Para retomarla basta un brief nuevo con esa historia (B2 contra la sección 7 de su spec) y quitarla de esta tabla.
