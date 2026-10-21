# Brief — SPEC-2 Catálogo e inventario

> Preparado por el lead · 2026-10-20 · Spec: [SPEC-2](../../specs/SPEC-2-catalogo-inventario.md) **v9** · Modelo de los implementadores: Sonnet · Ola 2a (en paralelo con las SPEC-1 y SPEC-3)
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Estrategia: [estrategia multiagente](../estrategia-multiagente.md)

Es la spec más grande (61 criterios), así que se construye en **dos pasos en secuencia sobre la misma rama** `spec/2-catalogo` (decisión del autor). Cada paso tiene su agente y su verificación por el lead.

| Paso | Qué | Alcance (V4) | Entra |
|---|---|---|---|
| **2.1** Backend | `catalog-service`, script de fotos, datos demo, k6 y Postman | [`SPEC-2.1.alcance`](SPEC-2.1.alcance) | Ahora |
| **2.2** Frontend | Pantallas de la tienda y del backoffice | [`SPEC-2.2.alcance`](SPEC-2.2.alcance) | 2.1 verificado por el lead, en la misma rama |

El PR a `dev` se abre al terminar 2.2, con la spec completa (V0b).

## Validación del brief (estrategia B2)

Copiada de la sección 7 de la SPEC-2 v9. Cuando un criterio tiene parte de API (dueño según la sección 7) y parte de pantalla, el otro paso agrega una prueba **complementaria** con el mismo id, sin quitarle el criterio a su dueño.

| CA | Sección 7: tipo · dónde | Dueño | Complementaria |
|---|---|---|---|
| 01–07 | Datos + API · `ProductoRepositoryTest` (filtros, `EXISTS`, `unaccent`), `CatalogoPublicoControllerTest` | 2.1 | 2.2: `ListadoPage` (03 filtros en la URL, 05 tarjeta, 06 vacío con "Limpiar filtros", 07 404) |
| 08–12 | Componente · `detalle.page.spec.ts`, `selector-talla.spec.ts` | 2.2 | 2.1: la API del detalle devuelve stock por variante y fotos por color |
| 13–16 | Unitaria + API · `CalculadoraDescuentosTest`, `CotizacionControllerTest` | 2.1 | — |
| 17–20 | Datos + API · `CategoriaRepositoryTest`, `CategoriaServiceTest` | 2.1 | 2.2: `ArbolCategorias` (18 arrastrar, 19 aviso del 409) y `EditorGuiaTallas` (20) |
| 21–28 | API · `ProductoServiceTest`, permisos OPERADOR | 2.1 | 2.2: `ProductoFormPage` (22 matriz, 24 `AvisoPublicacion`, 28 solo lectura para OPERADOR) |
| 29–32 | Integración · `ImagenServiceTest` con Garage en Testcontainers | 2.1 | 2.2: pestaña Imágenes (31 límite de 10, 32 color obligatorio, orden y principal) |
| 33–35 | API · `MaestrosControllerTest` | 2.1 | 2.2: `ListaTallas` y `ListaColores` |
| 36–38 | Unitaria + API · `DescuentoServiceTest` | 2.1 | 2.2: `EditorTramos` (36 errores, 37 etiquetas ocultas en la tienda) |
| 39–44 | Datos + API · `InventarioServiceTest`, `MovimientoStockRepositoryTest`, cruce de umbral | 2.1 | 2.2: `InventarioPage` (44 filtros), `DialogoEntrada`, `DialogoAjuste`, kardex (41) y mínimo (43) |
| 45–53 | Datos + integración · `StockRepositoryTest` (20 hilos), `ReservaServiceTest`, consumidores con RabbitMQ en Testcontainers | 2.1 | — |
| 54 | Integración · gateway (`/internal` → 404) | 2.1 | El gateway está congelado y su prueba de `/internal/**` ya existe. 2.1 la cubre en la colección Postman a través del gateway, con el id en el nombre |
| 55 | Carga · `k6/catalogo.js` | 2.1 | Se corre en Lyanna con la plataforma levantada; el resultado (p95 con y sin caché) va en el informe |
| 56–57 | Integración · invalidación de caché y Redis caído | 2.1 | — |
| 58–61 | Integración · `CargaMasivaServiceTest` (CSV y XLSX de ejemplo) | 2.1 | — (la pantalla P-2-09 pasó a [extensión de alcance](../extension-alcance.md)) |
| Todas | Postman · `postman/catalog.postman_collection.json` | 2.1 | — |

**Incluye (sección 1), repartido:**
- **2.1:** lógica completa de catalog, `StockNoConfirmado`, cachés, datos demo con su script de fotos, k6 y Postman.
- **2.2:** P-2-01 a P-2-08 y el botón "Agregar al carrito" (C1). P-2-09 (carga masiva) pasó a [extensión de alcance](../extension-alcance.md); su ruta sigue mostrando "Próximamente".

**No incluye:** la landing y sus destacados en pantalla (spec 3), el carrito y el checkout (spec 4), los correos de alertas (spec 7) ni el menú de la cabecera (0b).

`scripts/ca-cobertura.sh 2` debe dar **61 de 61** al cerrar 2.2. Los ids van del 01 al 61 (HU-2-12 usa del 58 al 61).

## Lectura obligatoria (los dos pasos, en este orden)

1. [`CLAUDE.md`](../../../CLAUDE.md).
2. [SPEC-2](../../specs/SPEC-2-catalogo-inventario.md) v9 completa. **La spec manda.**
3. Micro [`catalog-service.md`](../../analisis/micro/catalog-service.md) completo: D1–D8, invariantes, endpoints 4.1, flujos 4.2–4.9 (4.4.1 es la carga masiva y 4.5 la cotización), eventos y el "Diseño técnico" (`CalculadoraDescuentos`, `ReservaService`, `DisponibilidadPort`, paquetes `catalogo/` e `inventario/`).
4. Contratos:
   - [`catalog-service.yaml`](../../../contracts/openapi/catalog-service.yaml), incluido `/internal/v1/inventario/reservas`;
   - `comun.yaml`;
   - en [`fitstore-eventos.yaml`](../../../contracts/asyncapi/fitstore-eventos.yaml), los canales que publica catalog (`StockBajo`, `StockNoConfirmado`) y los que consume (`PedidoPagado`, `PedidoCancelado`, `PedidoExpirado`);
   - para 2.2, también `POST /carrito/items` de `order-service.yaml`.
5. El código de 0a de catalog (entidades, repositorios, `V1` y `V2`, pruebas como `StockConcurrenciaTest` y `PruebaRepositorio`) y [`docs/demo/ids-demo.md`](../../demo/ids-demo.md).
6. [`stack-versiones.md`](../../arquitectura/stack-versiones.md) v10:
   - Thumbnailator 0.4.21;
   - `imageio-webp` 3.15.2, que agregas al `pom.xml` de catalog;
   - POI 5.5.1 y Commons CSV 1.14.1;
   - Python 3.12, Pillow 12.3.0 y numpy 2.5.3 para el script.
7. **Solo 2.2:** la SPEC-0b (R2, R8, R11 y P-0b-04), `shared/src/public-api.ts` (`catalogApi`, `orderApi`, `CarritoContador`, `PrecioPipe`) y la `ui/` de cada app en su `/_ui`. Los diseños `Listado`, `ListadoKallpaMovil`, `Producto`, `ProductoKallpaMovil`, `ProductoForm` y `ProductoFormKallpa` están en [`docs/diseno/pantallas/`](../../diseno/pantallas/).

En tu primer mensaje: qué leíste, en 5–10 líneas qué construyes, y cómo probarás los criterios de concurrencia (CA-2-47) y de eventos (CA-2-49 a CA-2-53), o de pantallas en 2.2.

## 2.1 — Backend

**Qué construyes (por historia, con TDD):**
1. **Catálogo público:** listado con filtros, `unaccent` y orden (24 por página, máximo 48), detalle, guía de tallas heredada, destacados y categorías.
2. **Admin:**
   - categorías (2 niveles, orden, 409 al borrar con hijas o productos) y guía de tallas;
   - productos y variantes: matriz con el SKU de C2, `precioDesde`, publicar con la lista de faltantes, borrar o desactivar, destacado;
   - imágenes: tipo real del archivo; JPG, PNG o WebP de hasta 5 MB; salida JPG de 1200 y 400 px; máximo 10; color obligatorio con más de un color; exactamente una principal;
   - tallas y colores;
   - política de descuentos y modo por producto;
   - permisos (OPERADOR solo lee).
3. **Cotización** con la `CalculadoraDescuentos` (half-up por línea, "cuánto falta", líneas NO_DISPONIBLE, máximo 50 líneas).
4. **Inventario:**
   - entradas y ajustes;
   - kardex;
   - mínimo por SKU y la configuración de la tienda (`GET/PUT /admin/inventario/configuracion`, que usa la SPEC-3);
   - `StockBajo` una sola vez por cruce;
   - filtros "stock bajo" y "agotados".
5. **Reservas:** todo o nada con 409 y faltantes; idempotencia por `pedidoId`; consumidores idempotentes de `PedidoPagado`, `PedidoCancelado` y `PedidoExpirado`; `LiberadorReservasVencidas`; `StockNoConfirmado` sin descontar en negativo.
6. **Carga masiva:** plantilla CSV y XLSX con hoja de instrucciones; validar sin crear nada; confirmar todo o nada; entradas de stock; límites de 2 000 filas, 5 MB y 30 minutos.
7. **Cachés** en Redis con invalidación. El stock nunca sale de la caché. Si Redis se cae, se lee de la base.
8. **Datos demo:**
   - **`R__catalogo_demo.sql`** en `db/dev/`, con el árbol de categorías de la sección 6 y el color "Variado", unos 15 productos ACTIVO y 2 en BORRADOR, y stock variado.
   - **IDs explícitos** en los rangos de `ids-demo.md`, con `setval` al final.
   - **Fotos:**
     - `infra/scripts/semilla-imagenes/` con el script en Python + Pillow, `requirements.txt` con las versiones exactas, `CREDITOS.md` y las fotos fuente de licencia libre (Unsplash o Pexels);
     - el script recolorea cada prenda por color y deja una JPG por producto y color (1200 px, calidad 80) en `backend/catalog-service/src/main/resources/demo/imagenes/`;
     - al arrancar con `dev` o `demo`, catalog las sube a Garage con el mismo proceso de una subida del admin, si el bucket no las tiene;
     - las fotos y sus resultados deben pesar en total menos de 10 MB.
9. **k6** (`k6/catalogo.js`, 50 usuarios virtuales sobre listado y detalle) y **Postman** (`postman/catalog.postman_collection.json`, flujo admin y flujo tienda, con CA-2-54 a través del gateway).
10. `ContratoOpenApiTest` en verde contra `catalog-service.yaml`. El lead agrega `catalog-service` a `implementados.txt`.

**Núcleo (cobertura completa):** `CalculadoraDescuentos`, `ReservaService` (todo o nada, idempotencia, concurrencia), consumidores de eventos, `LiberadorReservasVencidas`, cruce de `StockBajo`, `ImagenService` (tipo real, tamaños, principal), `CargaMasivaService` (todo o nada), reglas de publicación y borrado.

## 2.2 — Frontend

**Qué construyes:**
- **Tienda, `features/catalogo`:**
  - **P-2-01 Listado** (`/productos` y `/categoria/:slug`): filtros en la URL (C3), chips, orden, paginador, tarjeta con "Desde", muestras, etiqueta de descuento y "Agotado", y los estados cargando, vacío y error.
  - **P-2-02 Detalle** (`/producto/:slug`):
    - galería solo del color elegido, con el primer color con stock preseleccionado;
    - tallas y colores sin stock tachados;
    - indicador de stock;
    - tramos de descuento;
    - guía de tallas;
    - cantidad que no supera el disponible;
    - **"Agregar al carrito"** con `orderApi` (`POST /carrito/items`, C1), que actualiza `CarritoContador` con la respuesta.
- **Backoffice, `features/catalogo`:**
  - **P-2-03 Productos:** `FsTabla` R11; ADMIN con menú "⋮" y OPERADOR con "Ver ›".
  - **P-2-04 Formulario:** pestañas Datos, Variantes, Imágenes y Descuento; `AvisoPublicacion`; solo lectura para OPERADOR.
  - **P-2-05 Categorías:** árbol arrastrable con CDK y la guía de tallas.
  - **P-2-06** Tallas y colores, y descuentos.
- **Backoffice, `features/inventario`:**
  - **P-2-07 Inventario:** filas marcadas por stock bajo y agotado con `claseFila`; el SKU abre el kardex.
  - **P-2-08 Kardex:** filas no clicables; el código de pedido enlaza a `/pedidos?codigo=…`.

**Rutas:** conserva los arreglos que importa el *shell*:
- **tienda:** `CATALOGO_ROUTES`;
- **backoffice:** `PRODUCTOS_ROUTES` (`''`, `nuevo`, `:id`), `CARGA_MASIVA_ROUTES` (se queda con su "Próximamente"), `CATEGORIAS_ROUTES`, `MAESTROS_ROUTES`, `DESCUENTOS_ROUTES` e `INVENTARIO_ROUTES` (`''`, `:varianteId`).

`/catalogo/productos/nuevo` es solo para ADMIN: pon `rolGuard('ADMIN')` (de `shared`) en esa subruta. Las demás guardas ya están en el *shell*.

**Reglas:**
- Usa los componentes de `ui/` de cada app; no crees los tuyos.
- Los endpoints se prueban con `HttpTestingController` contra el contrato (catalog ya está implementado en la rama; order todavía no).
- No cambies dependencias.

**Núcleo (cobertura completa):** filtros y URL del listado, selección de color y talla con su stock, agregar al carrito, `GeneradorMatriz`, `AvisoPublicacion` y `EditorTramos`.
