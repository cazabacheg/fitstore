# Brief — SPEC-3 Tienda y reclamos

> Preparado por el lead · 2026-10-20 · Spec: [SPEC-3](../../specs/SPEC-3-tienda-reclamos.md) **v10** · Modelo del implementador: Sonnet · Ola 2a (en paralelo con las SPEC-1 y SPEC-2)
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Estrategia: [estrategia multiagente](../estrategia-multiagente.md)

Un agente, **backend y luego frontend** (O3). Rama `spec/3-tienda`, alcance [`SPEC-3.alcance`](SPEC-3.alcance).

## Validación del brief (estrategia B2)

Copiada de la sección 7 de la SPEC-3 v10. Un solo paso: todos los criterios son de este agente. La columna "Nota" marca dónde hace falta también la prueba del otro lado (API o pantalla).

| CA | Sección 7: tipo · dónde | Dueño | Nota del lead |
|---|---|---|---|
| 01–05 | Componente · `inicio.page.spec.ts`, `carrusel-hero.spec.ts` | SPEC-3 | Destacados y categorías con `HttpTestingController` (catalog está en paralelo) |
| 06–08 | API · `ConfiguracionServiceTest` (RUC, versión optimista 409), `ImagenTiendaServiceTest` (tipos, SVG rechazado) | SPEC-3 | Más el componente de P-3-04 (mensaje del 409, error de RUC en su campo) |
| 09–12 | Unitaria + componente · `ContrasteWcagTest`, `editor-tema.spec.ts` | SPEC-3 | — |
| 13–16 | API + componente · validaciones de contenido, landing (máximo 5) | SPEC-3 | — |
| 17–19 | API · parámetros, `correosAlertas` fuera de `/settings/public` | SPEC-3 | — |
| 38 | Componente · `parametros.spec.ts` (catalog con HTTP de prueba; catalog caído no bloquea settings) | SPEC-3 | `PUT /admin/inventario/configuracion` lo implementa la SPEC-2 en paralelo: pruébalo con `HttpTestingController` contra el contrato |
| 20–22 | Datos + API · `PoliticaRepositoryTest`, inmutabilidad, `?version` | SPEC-3 | **CA-3-21 también en el frontend:** el Markdown se renderiza con `marked` y siempre pasa por `DOMPurify.sanitize`. Hace falta una prueba de componente de P-3-02 que demuestre que un `<script>` o un `onerror=` no llegan al DOM |
| 23–27 | Datos + API + integración · `ReclamoServiceTest`, `CalculadoraDiasHabilesTest`, concurrencia de 20, gateway 429 | SPEC-3 | **27:** el gateway está congelado y su límite de 3 por minuto por IP en `POST /reclamos` ya está configurado; se prueba en la colección Postman a través del gateway (el 4.º envío → 429). Más el componente de P-3-03 (24 apoderado, 25 número y fecha límite) |
| 28–31 | API · bandeja, transiciones, permisos OPERADOR | SPEC-3 | Más el componente de P-3-06 (OPERADOR sin "Tomar" ni "Responder", indicadores por vencer y vencido con `claseFila`) |
| 32–34 | Datos + API · `UbigeoRepositoryTest`, `Cache-Control` | SPEC-3 | — |
| 35–37 | Integración · caché e invalidación, Redis caído | SPEC-3 | — |
| Todas | Postman · `postman/settings.postman_collection.json` | SPEC-3 | — |

**Incluye (sección 1) y sección 8:** todo es de este agente: la lógica de settings, P-3-01 a P-3-06, los textos modelo de las 4 políticas, los datos demo y Postman.
- **Sin migración nueva:** `beneficios` ya está en la `V1` (SPEC-3 v10).
- El micro y las imágenes `docs/specs/img/P-3-*` ya están al día.

**No incluye:** correos del reclamo (spec 7), contador del dashboard (spec 8) ni feriados.

`scripts/ca-cobertura.sh 3` debe dar **38 de 38**.

## 1. Lectura obligatoria (en este orden)

1. [`CLAUDE.md`](../../../CLAUDE.md).
2. [SPEC-3](../../specs/SPEC-3-tienda-reclamos.md) v10 completa. **La spec manda.**
3. Micro [`settings-service.md`](../../analisis/micro/settings-service.md) completo: D1–D8, invariantes, endpoints 4.1, flujos 4.2–4.5, eventos, seguridad y el "Diseño técnico" (`ContrasteWcag`, `CalculadoraDiasHabiles`, correlativo con `INSERT … ON CONFLICT`, paquetes `configuracion/`, `ubigeo/` y `reclamaciones/`).
4. Contratos:
   - [`settings-service.yaml`](../../../contracts/openapi/settings-service.yaml) y `comun.yaml`;
   - en `fitstore-eventos.yaml`, los canales `ConfiguracionActualizada`, `ReclamoRegistrado` y `ReclamoRespondido`;
   - para el frontend, `GET /productos/destacados` y `GET /categorias` de `catalog-service.yaml` y `GET/PUT /admin/inventario/configuracion`.
5. El código de 0a de settings:
   - entidades y repositorios;
   - `V1` a `V3` (ubigeo INEI y configuración semilla);
   - pruebas como `CorrelativoReclamoRepositoryTest`;
   - su `application.yml` y las pruebas de `src/test/resources`. Su perfil de pruebas se llama `pruebas`; los otros 6 servicios usan `sin-config-server` (CK2 O5). Si lo alineas, que sea con el resto del servicio en verde.
6. La SPEC-0b (R2, R4, R11, P-0b-04) y `shared/src/public-api.ts` (`settingsApi`, `catalogApi`, `TemaService`, `derivarColores`, `contraste`, `UbigeoCatalogo`); la `ui/` de cada app en su `/_ui`. Diseños `Main`, `MainKallpaMovil`, `Configuracion` y `ConfiguracionKallpa` en [`docs/diseno/pantallas/`](../../diseno/pantallas/).
7. [`stack-versiones.md`](../../arquitectura/stack-versiones.md) v10: Thumbnailator 0.4.21, `imageio-webp` 3.15.2 (lo agregas al `pom.xml` de settings), `marked` 18.1.0 y `dompurify` 3.4.16 (ya están en `package.json`).
8. [`docs/demo/ids-demo.md`](../../demo/ids-demo.md).

En tu primer mensaje: qué leíste, en 5–10 líneas qué construyes, y cómo probarás CA-3-21 (Markdown sanitizado), CA-3-26 (concurrencia) y CA-3-27 (429).

## 2. Qué construyes

**Backend (`backend/settings-service`), por historia y con TDD:**
1. **Configuración por secciones** (identidad, tema, contenido, parámetros) con versión optimista (409), validaciones (RUC de 11 dígitos, enlaces del hero según S5, contacto, redes, 0 a 3 beneficios, rangos de parámetros) y `ConfiguracionActualizada`. Los correos para alertas viajan en el evento pero nunca salen en `/settings/public`.
2. **`/settings/public`** con caché en Redis, que se invalida al guardar cualquier sección o imagen. Si Redis se cae, se lee de la base. Nunca expone datos internos.
3. **`ContrasteWcag`** (advertencia; nunca bloquea el guardado).
4. **Imágenes de tienda:**
   - logos en PNG o WebP y favicon en PNG o ICO, hasta 2 MB, con el tipo real validado (nunca SVG);
   - landing en JPG o WebP de hasta 5 MB, redimensionada a 1600 px y guardada en JPG; máximo 5, con orden;
   - todo en el bucket público.
5. **Políticas versionadas e inmutables**, y los textos modelo de la versión 1 marcados como plantilla a revisar.
6. **Libro de Reclamaciones:** correlativo atómico por año (`RC-2026-000015`), 15 días hábiles, transiciones (EN_ATENCION opcional; RESPONDIDO solo ADMIN e inmutable) y los eventos.
7. **Ubigeo:** listas ordenadas con `Cache-Control` de 24 h, buscador sin tildes y nombre completo de un código.
8. **Datos demo** (`db/dev/`, perfiles `dev` y `demo`):
   - beneficios, contacto y tienda física en Gamarra;
   - 3 reclamos (vencido, por vencer y respondido), con fechas relativas al momento de la carga;
   - **3 imágenes de landing** de licencia libre: van en `backend/settings-service/src/main/resources/demo/imagenes/` con su `CREDITOS.md`, y settings las sube a Garage al arrancar con `dev` o `demo` si el bucket no las tiene. Es el mismo mecanismo que catalog (SPEC-2 v9, decisión del autor), con menos de 3 MB en total.
9. `ContratoOpenApiTest` en verde contra `settings-service.yaml`. El lead agrega `settings-service` a `implementados.txt`.

**Frontend:**
- **Tienda:**
  - `features/inicio` (P-3-01), con:
    - `CarruselHero` (cada 6 s, se pausa con el mouse o el foco, se maneja por teclado y respeta `prefers-reduced-motion`);
    - la franja de beneficios;
    - las categorías raíz;
    - los destacados (máximo 8), con una tarjeta propia en `inicio/` con la misma estructura que la del listado;
    - la tienda física;
    - los estados de la sección 4.
  - `features/legal`: P-3-02 Política y P-3-03 Libro de Reclamaciones, con la confirmación imprimible.
- **Backoffice:**
  - `features/configuracion`: P-3-04, con pestañas, vista previa del tema sin guardar, "Restaurar tema por defecto", aviso de cambios sin guardar y el bloque Inventario contra catalog (CA-3-38); y P-3-05 Políticas, con el editor Markdown, la vista previa sanitizada y el historial.
  - `features/reclamos`: P-3-06 con `FsTabla` R11 y el detalle.

**Rutas:** conserva los arreglos que importa el *shell*:
- **tienda:** `INICIO_ROUTES` y `LEGAL_ROUTES` (`libro-de-reclamaciones`, `politicas/:tipo`);
- **backoffice:** `CONFIGURACION_ROUTES` (`''`, `politicas`) y `RECLAMOS_ROUTES` (`''`, `:id`).

**Postman:** `postman/settings.postman_collection.json`, con CA-3-27 a través del gateway.

## 3. Reglas propias

- **Markdown (S3):** siempre `marked` → `DOMPurify.sanitize` → `[innerHTML]`. Nunca uses `bypassSecurityTrust*`. El backend guarda el texto tal cual.
- **Marca en vivo:** el DoD de esta spec incluye verificar en vivo que la tienda y el backoffice arrancan con la marca de los datos semilla. Lo hace el lead con la plataforma levantada; tú deja `/settings/public` respondiendo con la semilla de `V3`.
- **Congelados:** `shared`, `ui/`, `layout/`, `app.routes.ts`, gateway y `fitstore-common`. El catálogo es de la SPEC-2: **no toques `backend/catalog-service`**.

**Núcleo (cobertura completa):** `ContrasteWcag`, `CalculadoraDiasHabiles`, correlativo de reclamos (concurrencia), transiciones del reclamo, validaciones de cada sección con la versión optimista, `ImagenTiendaService` (tipos reales, SVG), caché de `/settings/public` y lo que excluye, y en el frontend el carrusel, el render sanitizado del Markdown y el guardado de Parámetros con catalog caído.
