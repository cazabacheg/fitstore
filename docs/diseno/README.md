# FitStore — Diseño de alta fidelidad

> Versión: **v1 (aprobado)** — 2026-10-19 · Etapa: specs ✅ → contratos ✅ → **diseño ✅** → estrategia multiagente → implementación.

Copia versionada del diseño aprobado. Los originales, que se ven y se editan en el navegador, son dos artifacts de Claude:

| Qué | Dónde | Copia aquí |
|---|---|---|
| Sistema de diseño "FitStore" (tokens, guía de marca y 14 componentes) | [artifact](https://claude.ai/artifact/Et7tCDyQaEnLzja8GH4zxT) | [`sistema/`](sistema/) |
| Canvas "FitStore Tienda" (14 pantallas clave en dos marcas y las piezas compartidas) | [artifact](https://claude.ai/artifact/1iacKddHAquNYuQcFPBZLg) | [`pantallas/`](pantallas/) |

Si se cambia un artifact, se vuelve a copiar aquí en el mismo PR que ajuste las specs.

## Decisiones de la etapa

| # | Decisión | Detalle |
|---|---|---|
| D1 | **Dos marcas para demostrar el white-label** | FitStore (plantilla: arena `#C9B28C`, espresso `#2B2621`, olivo `#4E5B45`) y Kallpa (marca de prueba: frambuesa `#B3245C`, berenjena `#22182E`, lima `#A3E635`). Se cambian con `data-theme` |
| D2 | **Entre marcas solo cambian los colores** | Tipografía Poppins y radios 4 / 8 / 16 px fijos; la tienda configura solo sus cinco colores (SPEC-0b R4, SPEC-3 HU-3-03, micro settings D6) |
| D3 | **Colores derivados** | `fs-color-en-primario` (blanco o el color de texto, el de más contraste) y `fs-color-primario-texto` (el primario oscurecido hasta 4.5:1 para texto y enlaces). Así un primario claro como el arena es válido |
| D4 | **Cabecera de la tienda clara** | `fs-color-superficie` con línea inferior; hasta 4 categorías en mayúsculas espaciadas; la actual subrayada y las demás se atenúan al pasar el mouse (SPEC-0b CA-0b-20c). El secundario queda para el pie, el hero y la barra lateral del backoffice |
| D5 | **Hero editorial a todo el ancho** | Imagen de fondo de 1600 px con el texto encima, flechas e indicadores del carrusel (SPEC-3 CA-3-02); en móvil, más bajo |
| D6 | **Móvil siempre en una columna** | Los bloques de dos columnas pasan a una bajo 720 px aunque la pantalla ajuste sus columnas de escritorio; listado en 2 columnas de tarjetas iguales |
| D7 | **Iconos Material Symbols Rounded** | El mismo set que usa Angular Material en el backoffice |

## Pantallas

| Spec | Pantalla | FitStore | Kallpa |
|---|---|---|---|
| 0b | P-0b-01 Cabecera y pie de la tienda | `TiendaCabecera`, `TiendaPie` | (mismas piezas) |
| 0b | P-0b-02 Barra lateral del backoffice | `BoBarra` | (misma pieza) |
| 3 | P-3-01 Landing | `Main` | `MainKallpaMovil` |
| 2 | P-2-01 Listado | `Listado` | `ListadoKallpaMovil` |
| 2 | P-2-02 Detalle de producto | `Producto` | `ProductoKallpaMovil` |
| 4 | P-4-01 Carrito | `Carrito` | `CarritoKallpaMovil` |
| 4 | P-4-02 Checkout | `Checkout` | `CheckoutKallpaMovil` |
| 5 | P-5-01 Pago | `Pago` | `PagoKallpaMovil` |
| 5 | P-5-02 Resultado del pago | `PagoResultado` | `PagoResultadoKallpaMovil` |
| 4 | P-4-03 Mis pedidos | `MisPedidos` | `MisPedidosKallpaMovil` |
| 4 | P-4-04 Detalle de mi pedido | `MiPedido` | `MiPedidoKallpaMovil` |
| 8 | P-8-01 Dashboard | `Dashboard` | `DashboardKallpa` |
| 4 | P-4-05 Pedidos | `Pedidos` | `PedidosKallpa` |
| 4 | P-4-06 Detalle de pedido | `Pedido` | `PedidoKallpa` |
| 2 | P-2-04 Formulario de producto | `ProductoForm` | `ProductoFormKallpa` |
| 3 | P-3-04 Configuración de tienda | `Configuracion` | `ConfiguracionKallpa` |

La tienda se diseñó en escritorio (FitStore) y móvil de 390 px (Kallpa); el backoffice, en escritorio de 1440 px en las dos marcas. El resto de pantallas deriva del sistema de diseño y de su boceto de baja fidelidad.

## Cómo usar esta copia en la implementación

- `sistema/tokens.json` es la fuente de los valores: el frontend escribe los cinco colores de settings como variables CSS y deriva el resto (SPEC-0b R4); tipografía, espacios, radios y sombras son constantes de `ui/`.
- `sistema/components/*/README.md` describe cada componente de `ui/` y `bundle.css` muestra sus estilos de referencia (clases `fs-*`, solo con tokens).
- `pantallas/*.dc.html` son las pantallas del canvas: HTML con estilos en línea y datos de ejemplo. Se abren en el artifact (necesitan su `support.js`); aquí sirven de referencia de estructura, textos y medidas. `marca.css` tiene las variables de las dos marcas y las clases de página (`fs-pagina`, rejillas, `fs-bo`, línea de tiempo, selectores).
- Los textos de las pantallas siguen la guía de tono del sistema (español de Perú, tuteo, sin emoji).
