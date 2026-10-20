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
3. **Clientes de API** (R5): `npm run api:generate` con `openapitools/openapi-generator-cli:v7.10.0` (`typescript-angular`) → `shared/api/<servicio>/`, versionados en Git.
4. **Job `frontend` en `.github/workflows/ci.yml`**: lint, pruebas con cobertura y build, con filtro por ruta (solo si cambia `frontend/` o `contracts/`) y caché de npm; y la verificación de clientes desactualizados (CA-0b-30).

### 3. Fuera de alcance de 0b.1

Layouts, componentes de `ui/`, rutas de las features y `/_ui` de cada app (0b.2 y 0b.3); pantallas de login (spec 1); todo `backend/`, `infra/` y `contracts/`.

---

## 0b.2 — Storefront y 0b.3 — Backoffice

Se completan al cerrar 0b.1 (como 0a.2), con lo aprendido: ejemplos reales de `shared`, rutas exactas y trampas encontradas. Resumen de lo que construye cada uno:

- **0b.2 Storefront:** `TiendaShell` (P-0b-01: cabecera clara con hasta 4 categorías y atenuación al pasar el mouse, menú lateral en móvil, pie al final del documento), `ui/` de la tienda con Tailwind (P-0b-04, columna izquierda), estados comunes (P-0b-03), rutas R2 de la tienda con "Próximamente", `/_ui` solo en desarrollo. Diseño: `docs/diseno/pantallas/TiendaCabecera.dc.html`, `TiendaPie.dc.html` y `marca.css`.
- **0b.3 Backoffice:** `BackofficeShell` (P-0b-02: menú lateral R3 por rol con contadores, barra superior con migas y menú de usuario), `ui/` del backoffice con Angular Material (P-0b-04, columna derecha, con `FsTabla` según R11), estados comunes, rutas R2 del backoffice con "Próximamente" y página 403, `/_ui` solo en desarrollo. Diseño: `docs/diseno/pantallas/BoBarra.dc.html` y las barras superiores de las pantallas del backoffice.

## DoD de la marca (decisión del autor, 2026-10-20)

`/settings/public`, `/categorias`, `/staff/me` y `/ubigeo` los implementan las specs 1, 2 y 3. En 0b la marca se verifica con las pruebas (respuesta simulada con `HttpTestingController`) y en vivo con el tema FitStore por defecto (CA-0b-07); la verificación en vivo con los datos semilla está en el DoD de la SPEC-3 (SPEC-0b v10, SPEC-3 v9).
