# Brief — SPEC-0b Fundaciones del frontend

> Preparado por el lead · 2026-10-20 · Spec: [SPEC-0b](../../specs/SPEC-0b-fundaciones-frontend.md) **v10** · Modelo de los implementadores: Sonnet
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Estrategia: [estrategia multiagente](../estrategia-multiagente.md)

La SPEC-0b se construye en tres pasos. El primero deja el workspace y todo lo compartido; los otros dos son las dos aplicaciones, **en paralelo** porque viven en carpetas distintas y no comparten archivos.

| Paso | Rama | Alcance (V4) | Entra |
|---|---|---|---|
| **0b.1** Workspace, `shared` y CI | `spec/0b-1-base` | [`SPEC-0b.1.alcance`](SPEC-0b.1.alcance) | Ahora |
| **0b.2** Storefront (tienda) | `spec/0b-2-storefront` | [`SPEC-0b.2.alcance`](SPEC-0b.2.alcance) | 0b.1 en `dev` |
| **0b.3** Backoffice | `spec/0b-3-backoffice` | [`SPEC-0b.3.alcance`](SPEC-0b.3.alcance) | 0b.1 en `dev` (a la vez que 0b.2) |
| **CK2** Checkpoint de base (0a + 0b) | — | — | 0b.2 y 0b.3 en `dev` |

**Regla para que 0b.2 y 0b.3 no choquen:** 0b.1 deja **todo** lo que es del workspace (`angular.json`, `package.json` con todas las dependencias de las dos apps, `tsconfig*`, ESLint, Prettier, proxy, Tailwind configurado para la tienda, Angular Material instalado para el backoffice, presupuestos de tamaño, locale `es-PE`) y el esqueleto generado de cada app. Después, cada app solo toca su carpeta `frontend/projects/<app>/`.

### Validación del brief (estrategia B2)

Copiada de la sección 7 de la SPEC-0b v10. Cuando la sección 7 prueba un criterio **por app** ("pruebas de rutas y guards por app", componentes de `ui/` de cada app), cada app es dueña de su parte, con su propio archivo de prueba.

| CA | Sección 7: tipo · dónde | Dueño | Complementaria |
|---|---|---|---|
| 01, 02 | Build / CI · `npm run build`, `npm run lint`, `npm test` | 0b.1 (0b.2 y 0b.3 los mantienen en verde) | — |
| 03 | Manual · `ng serve` + plataforma 0a | 0b.1 | — |
| 04 | CI · job `frontend` en `ci.yml` (filtro por ruta y caché de npm) | 0b.1 | — |
| 05–08 | Unitaria + manual · `shared/theme/*.spec.ts` | 0b.1 | 0b.2 y 0b.3: que su app use las variables (Tailwind `@theme`, tokens de Material) |
| 09–14 | Unitaria · `shared/http/*.spec.ts` (`HttpTestingController`) | 0b.1 | 0b.2 y 0b.3: toast y banner de su `ui/` reaccionan al error (CA-0b-13) |
| 15, 16, 19 | Unitaria · rutas y guards **por app** | Parte storefront → 0b.2 · parte backoffice → 0b.3 | 0b.1: pruebas de los guards de `shared` |
| 17, 18 | Unitaria · rutas y guards del backoffice | 0b.3 | — |
| 20, 20b, 20c, 21, 22, 23 | Componente · `TiendaShell` (incluye 390 px y el límite de 4 categorías) | 0b.2 | — |
| 24 | Componente · `BackofficeShell` | 0b.3 | — |
| 25 | Componente · `/_ui` **en cada app** | Parte storefront → 0b.2 · parte backoffice → 0b.3 | — |
| 26 | Componente · selector de ubigeo de `ui/` **en cada app** | Parte storefront → 0b.2 · parte backoffice → 0b.3 (con `hasta`) | 0b.1: servicio de ubigeo de `shared` con caché |
| 27 | Componente · pipe de precio | 0b.1 (`shared/pipes`) | — |
| 28, 28b | Componente · `fs-tabla.spec.ts` | 0b.3 | — |
| 29, 30 | Script · `npm run api:generate` en CI | 0b.1 | — |

Todos los criterios tienen dueño; `scripts/ca-cobertura.sh 0b` debe dar **33 de 33** al cerrar 0b.3.

---

## 0b.1 — Workspace, `shared` y CI

### 1. Lectura obligatoria (en este orden)

1. [`CLAUDE.md`](../../../CLAUDE.md).
2. [SPEC-0b](../../specs/SPEC-0b-fundaciones-frontend.md) completa, en especial la sección 1, 3 (HU-0b-01, 02, 03, 07 y los guards de HU-0b-04), 5 (R1 a R11) y 7–9.
3. Los micros que cita la sección 2 de la spec: auth 4.4 (sesión, refresh por cookie), settings 4.1–4.4 (configuración pública, ubigeo), infraestructura 4 y 5, order 4.2 (`X-Carrito-Id`).
4. [`contracts/README.md`](../../../contracts/README.md) (K4 a K9) y `contracts/openapi/*-service.yaml` (de ahí se generan los clientes).
5. El sistema de diseño en [`docs/diseno/sistema/`](../../diseno/sistema/): `README.md` (marca configurable, colores derivados) y `tokens.json`.
6. [`stack-versiones.md`](../../arquitectura/stack-versiones.md) (Node 22, Angular 22.2.x, Tailwind 4.3.x, OpenAPI Generator `v7.10.0`).

### 2. Qué construyes

1. **Workspace** `frontend/` con Angular 22.2.x: aplicaciones `storefront` y `backoffice` (esqueleto generado por el CLI) y librería `shared`; `angular.json` completo para las dos apps (Tailwind 4.3 en la tienda, Angular Material 22 en el backoffice, presupuestos, locale `es-PE`, proxy de desarrollo a `localhost:8080`); ESLint, Prettier, Vitest con `@vitest/coverage-v8` y umbral de **80 % de líneas** por proyecto (sin `shared/api`).
2. **`shared`** según R1: `api` (generado), `auth` (`SesionStore`, guards `autenticadoGuard` y `rolGuard`), `http` (los 5 interceptores en el orden de R7), `theme` (R4: carga de `/settings/public`, variables CSS, colores derivados, respaldo en `localStorage`, título y favicon), `i18n` (`es-PE`), `ubigeo` (servicio con caché), `carrito` (`CarritoContador`), `models` (`ApiResponse`, página), `pipes` (`precio`, `fecha`), y el token `CONTADOR_MENU` (R3).
3. **Clientes de API** (R5): `npm run api:generate` con `openapitools/openapi-generator-cli:v7.10.0` (`typescript-angular`) → `shared/src/lib/api/<servicio>/`, versionados en Git.
4. **Job `frontend` en `.github/workflows/ci.yml`**: lint, pruebas con cobertura y build, con filtro por ruta (solo si cambia `frontend/` o `contracts/`) y caché de npm; y la verificación de clientes desactualizados (CA-0b-30).

### 3. Fuera de alcance de 0b.1

Layouts, componentes de `ui/`, rutas de las features y `/_ui` de cada app (0b.2 y 0b.3); pantallas de login (spec 1); todo `backend/`, `infra/` y `contracts/`.

---

## Común a 0b.2 y 0b.3

> Completado tras la entrega de 0b.1 (PR #33). Dos agentes a la vez, cada uno en su carpeta.

### Lectura obligatoria (en este orden)

1. [`CLAUDE.md`](../../../CLAUDE.md).
2. Este brief: la regla para que no choquen, la tabla de validación y la sección de tu paso.
3. [SPEC-0b](../../specs/SPEC-0b-fundaciones-frontend.md) (v11) completa. **La spec manda.** En especial las historias de tu app, la sección 4 (pantallas), la 5 (R1, R2, R3, R10, R11) y la 7.
4. El sistema de diseño: [`docs/diseno/sistema/README.md`](../../diseno/sistema/README.md), `components/*/README.md` (cada componente de `ui/` que construyes tiene su ficha) y `components/bundle.css` (estilos de referencia, clases `fs-*`).
5. Las pantallas aprobadas de tu app en [`docs/diseno/pantallas/`](../../diseno/pantallas/) y `marca.css`.
6. **El código de 0b.1 ya integrado:** `frontend/projects/shared/src/public-api.ts` (todo lo que exporta `shared`: `SesionStore`, guards, `ToastService`, `EstadoServicio`, `TemaService`, `UbigeoCatalogo`, `PrecioPipe`, `FechaPipe`, `CarritoContador`, `CONTADOR_MENU`, clientes `authApi`, `catalogApi`, …), `shared/src/lib/theme/tokens.css` y el `app.config.ts` y `styles.css` de tu app.

En tu primer mensaje: qué leíste, qué componentes y rutas vas a construir y cómo probarás cada criterio tuyo.

### Reglas

- Toca **solo** `frontend/projects/<tu-app>/`. `shared`, `angular.json`, `package.json` y `ci.yml` están congelados: si necesitas algo de ahí (una dependencia, un export de `shared`), **detente y pregúntame**.
- **Tus criterios necesitan pruebas propias dentro de tu carpeta**, con el id en el nombre, aunque `shared` ya tenga una prueba complementaria con el mismo id (el lead lo verifica buscando en tu carpeta, no solo con `ca-cobertura.sh`).
- Los endpoints que consume tu *shell* (`/categorias`, `/settings/public`, `/staff/me`, `/ubigeo`) llegan con las specs 1–3: pruébalos con `HttpTestingController` y deja los estados de fallo de la sección 4 funcionando (el *shell* no se bloquea si fallan).
- Cada ruta de la tabla R2 de tu app crea su carpeta `features/<carpeta>/<carpeta>.routes.ts` con la página "Próximamente" (R2): la spec dueña solo reemplaza esa carpeta.
- `/_ui` solo en desarrollo (`isDevMode()` o la configuración de desarrollo); en el build de producción la ruta no existe (CA-0b-25).
- Cobertura ≥ 80 % de líneas en tu proyecto (lo exige el build); núcleo cubierto por completo (abajo).
- Lecciones de 0b.1: `npm ci` funciona con npm 10, pero **no cambies dependencias** (el `package-lock.json` exige `npx npm@11`); los clientes de API se usan por espacio de nombres (`catalogApi.CategoriasService`…); los tipos comunes están en `shared/models`.

## 0b.2 — Storefront

Rama `spec/0b-2-storefront`, alcance [`SPEC-0b.2.alcance`](SPEC-0b.2.alcance).

**Qué construyes:**
1. **Tema de Tailwind:** `@theme` en `styles.css` con las variables de `tokens.css` (colores de marca y derivados, tipografía, espacios, radios, sombras), para que las clases usen la marca de settings (CA-0b-05).
2. **`TiendaShell` (P-0b-01)** según `TiendaCabecera.dc.html` y `TiendaPie.dc.html`: cabecera clara con logo, **hasta 4 categorías raíz** de `GET /categorias` en su orden, atenuación de las demás al pasar el mouse o el foco y la actual subrayada (CA-0b-20c), buscador que navega a `/productos?q=` (CA-0b-22), cuenta y carrito con el `CarritoContador` (CA-0b-23); en 390 px, menú lateral ☰ junto al logo con todas las categorías y la cuenta, sin scroll horizontal (CA-0b-21); pie al final del documento, nunca fijo, con el contenido de altura mínima de la ventana (CA-0b-20b) y el enlace "Libro de Reclamaciones" (CA-0b-20); `BarraAnuncio`.
3. **`ui/` de la tienda (P-0b-04, columna izquierda):** botón, campo, select, checkbox, selector de ubigeo (CA-0b-26), precio con descuento, tarjeta de producto (esqueleto), badge, paginador simple, galería básica, estados, toast, diálogo. Cada uno según su ficha del sistema de diseño, con estados de foco y accesibilidad (R10).
4. **Estados comunes (P-0b-03):** `EstadoCargando`, `EstadoVacio`, `EstadoError`, `Pagina404` con "Volver al inicio" (CA-0b-19), `BannerServicioNoDisponible` (reacciona a `EstadoServicio`, CA-0b-13), `Toast` (reacciona a `ToastService`), `DialogoConfirmacion`.
5. **Rutas R2 de la tienda** con carga diferida, "Próximamente" (CA-0b-15) y guards: `/checkout`, `/pago/:codigo`, `/cuenta/**` con `autenticadoGuard` → `/ingresar?returnUrl=` (CA-0b-16); ruta comodín → 404.
6. **`/_ui`** con todos los componentes de `ui/`, sus variantes y estados (CA-0b-25).

**Criterios:** CA-0b-20, 20b, 20c, 21, 22, 23 (componente), la parte storefront de 15, 16, 19, 25 y 26; complementarias de 05, 08 (logo en la cabecera) y 13 (toast y banner).

**Núcleo:** `TiendaShell` (límite de 4 categorías, estado de fallo de `/categorias`, menú móvil, pie), guards de rutas y el selector de ubigeo.

## 0b.3 — Backoffice

Rama `spec/0b-3-backoffice`, alcance [`SPEC-0b.3.alcance`](SPEC-0b.3.alcance).

**Qué construyes:**
1. **Tema de Angular Material 3:** mapeo de los tokens de `tokens.css` a `--mat-sys-*` (primario, superficie, texto, error…) en `styles.css`, para que los componentes de Material usen la marca de settings (CA-0b-05).
2. **`BackofficeShell` (P-0b-02)** según `BoBarra.dc.html` y la barra superior de las pantallas del backoffice: `sidenav` con el **menú R3** por rol (OPERADOR no ve Pagos, Tarifas y agencias, Staff, Notificaciones ni Configuración de tienda — CA-0b-17), ítem activo resaltado, subítems, **insignia de contador** para los ítems con un `CONTADOR_MENU` registrado, refrescada al navegar (CA-0b-24); colapsable en tablet; `BarraSuperior` con migas y menú de usuario (perfil, cambiar contraseña, salir); `EncabezadoPagina` (título + acción principal).
3. **`ui/` del backoffice (P-0b-04, columna derecha):** `FsTabla` (`mat-table` + paginador + orden que emiten `page/size/sort` del ADR-015 — CA-0b-28; y la convención **R11** con `filaEnlace` y `acciones` — CA-0b-28b), barra de filtros, selector de ubigeo con la opción `hasta` (CA-0b-26), subida de imágenes (arrastrar, validar tipo y tamaño), badge de estado (pedido, pago, entrega), estados, toast, diálogo de confirmación.
4. **Estados comunes (P-0b-03):** los mismos de la tienda con Material, más `Pagina403`.
5. **Rutas R2 del backoffice** con carga diferida, "Próximamente" (CA-0b-15), `autenticadoGuard` (solo STAFF; un CLIENTE va al login de staff — CA-0b-18) y `rolGuard('ADMIN')` en las rutas de ADMIN → `/403` (CA-0b-16, 17); `/ingresar`, `/recuperar`, `/restablecer` públicas; comodín → 404 (CA-0b-19).
6. **`/_ui`** con todos los componentes de `ui/` (CA-0b-25).

**Criterios:** CA-0b-17, 18, 24, 28, 28b, la parte backoffice de 15, 16, 19, 25 y 26; complementarias de 05, 08 y 13.

**Núcleo:** filtrado del menú por rol y contadores, `FsTabla` (R11: enlace, clic en la fila, acciones que no propagan, tabla sin detalle), guards de rutas y el selector de ubigeo con `hasta`.

## DoD de la marca (decisión del autor, 2026-10-20)

`/settings/public`, `/categorias`, `/staff/me` y `/ubigeo` los implementan las specs 1, 2 y 3. En 0b la marca se verifica con las pruebas (respuesta simulada con `HttpTestingController`) y en vivo con el tema FitStore por defecto (CA-0b-07); la verificación en vivo con los datos semilla está en el DoD de la SPEC-3 (SPEC-0b v10, SPEC-3 v9).
