# Checkpoint CK2 — Base completa (0a + 0b)

> Lead · 2026-10-20 · Revisión exhaustiva antes de lanzar la ola 2 (specs 1–7) en paralelo ([estrategia 2.1](../estrategia-multiagente.md#21-checkpoints-de-base)).
> Base revisada: `dev` en `e5a989d` (PR #36). Contraste: **spec primero** (SPEC-0a v9, SPEC-0b v11), luego los micros (infraestructura v9 y los 7 de servicio) y los contratos.

## Resultado

**Aprobable con correcciones.** La base compila y pasa todas sus pruebas desde cero. Hay **dos bloqueantes para el paralelo**:

- **H1:** las rutas del backoffice impiden que las specs 1, 2, 5 y 6 armen sus pantallas sin tocar `app.routes.ts`, que queda congelado.
- **H2:** el *smoke* falla con un `.env` nuevo, y con él fallaría la promoción a `stage`.

Además hay dos correcciones importantes y tres menores. Todas se corrigen en `fix/ck2-base` y en un PR de documentación **antes** de lanzar la ola 2.

## 1. Desde cero

Clon limpio de `dev` en una carpeta nueva:

| Verificación | Resultado |
|---|---|
| `./mvnw -B -fae clean verify` | ✅ BUILD SUCCESS, 11 módulos, **562 pruebas** sin fallos |
| JaCoCo, líneas por módulo (mínimo 80 %) | ✅ common 92,8 · gateway 98,0 · auth 83,9 · catalog 98,6 · settings 100 · order 100 · payment 96,1 · delivery 100 · notification 100 |
| `npm ci` | ✅ 496 paquetes, 0 vulnerabilidades |
| `npm run lint` | ✅ limpio |
| `npm run build` | ✅ sin avisos de presupuesto; inicial: storefront 396,75 kB, backoffice 348,17 kB (límite de aviso 500 kB) |
| Vitest | ✅ shared 103 pruebas, 99,33 % de líneas · storefront 127, 98,67 % · backoffice 186, 97,6 % |
| `infra/smoke.sh` (compose de dev completo) | ❌ **H2**. Arranque *healthy* en 134 s (límite 300 s), Eureka con 8 servicios, Flyway aplicado, aislamiento de bases, Swagger con 7 servicios y memoria 4 448 MiB de 6 198 MiB, pero las 7 verificaciones de `ddl-auto=validate` fallan porque el script no carga las variables (ver H2). Con H2 corregido pasa completo (sección 8) |

## 2. Código contra la spec y el micro

| Pieza | Spec / micro | Resultado |
|---|---|---|
| `shared` (sesión, interceptores, guards, tema, ubigeo, pipes, contador del carrito, `CONTADOR_MENU`) | SPEC-0b R4–R8, auth 4.4, settings 4.4 | ✅ Interceptores en el orden de R7. El refresco es único y compartido, y reintenta una sola vez. El token vive solo en memoria y en `localStorage` solo se guardan la configuración y el `carritoId`. Las rutas de login y refresh no disparan otro refresco, contrastado con los paths de `auth-service.yaml`. ⚠️ **H5** (caché de búsquedas de ubigeo) |
| Storefront: rutas, *shell*, cabecera, pie, menú móvil | SPEC-0b R2, P-0b-01, CA-0b-15 a 23 | ✅ Cada ruta de R2 carga su carpeta con exportaciones con nombre (`CATALOGO_ROUTES`, `CUENTA_ROUTES`…). Las rutas de las specs 1–5 (sección 4 de cada una) caen en la carpeta y la subruta correctas |
| Backoffice: rutas | SPEC-0b R2 y las rutas de las pantallas de las specs 1, 2, 3, 4, 5, 6, 7 y 8 | ❌ **H1** |
| Backoffice: menú R3, migas, barra superior, `/403`, `/404` | SPEC-0b R3, P-0b-02, CA-0b-16 a 19 y 24 | ✅ Ítems, subítems y rol mínimo idénticos a R3, y los roles coinciden con el acceso de cada pantalla de las specs 2, 3, 5, 6 y 7. Contador por `CONTADOR_MENU` |
| Backoffice `ui/` (tabla R11, filtros, selector de ubigeo, subida de imágenes, insignias, estados, toast, diálogo) | SPEC-0b P-0b-04, R11 | ✅ Las insignias cubren todos los valores de `EstadoPedido`, `EstadoIntencion`, `EstadoTransaccion`, `EstadoEntrega` y `EstadoReembolso` de los contratos. ⚠️ **H6** (cursor de las filas `aviso`/`error`) |
| Tema Material 3 y Tailwind | SPEC-0b R4, CA-0b-05 | ✅ `--mat-sys-*` y `@theme` usan las variables que escribe el `TemaService` |
| Esqueletos de los 7 servicios | SPEC-0a 0a.2, micro de cada servicio | ✅ Paquetes iguales al "Diseño técnico" de cada micro. `outbox_evento` y `evento_procesado` con los tipos de infraestructura 10.3. `CatalogoRutasEventos` con los canales del AsyncAPI. `ContratoOpenApiTest` en los 7. ⚠️ **H4** (imágenes de Testcontainers) y **O5** (settings) |
| CI (`ci.yml`) y scripts | SPEC-0a HU-0a-06, SPEC-0b CA-0b-04/29/30 | ✅ Filtros por ruta, caché, gitleaks con huella, contratos y frontend. ⚠️ **H3** (`implementados.txt`) |

## 3. Contratos

Sin cambios desde CK1. `ApiResponse`, `ApiError`, `Pagina` y el sobre de eventos son idénticos a K4–K6 y K12, también en los modelos de `shared/models`. Los clientes generados están al día: los verifica el job de CI `verificar-clientes.sh`.

## 4. Pruebas y cobertura

- **Backend:** se leyeron los nombres y el propósito de las pruebas de los esqueletos. Cada servicio prueba su esquema (`EsquemaInicialTest` o `EsquemaBaseTest`), cada repositorio, las rutas de eventos, la configuración dev/cloud y el contrato.
- **Frontend:** se leyeron todas las pruebas de `shared` y las de rutas y *shell* de las dos apps. CA-0b-10 prueba tres peticiones simultáneas con un solo refresh, y CA-0b-17 prueba el menú y el acceso por URL.
- Ningún `skip`, `xit` ni `@Disabled`.
- **Hueco que dejó pasar H1:** las pruebas de rutas del backoffice comprueban que cada URL muestre "Próximamente", pero no que cada URL llegue a **su propia** subruta.

## 5. Seguridad

✅ gitleaks limpio en el CI de #35 y #36. Sin tokens en `localStorage`. El `/_ui` de las dos apps no está en el build de producción: verificado en `dist/`.

## 6. Lista para el paralelo

| Requisito | Estado |
|---|---|
| Cada spec trabaja solo en su carpeta de `features/` y en su servicio | ❌ hasta corregir H1. Con las rutas actuales, las specs 1, 2, 5 y 6 tendrían que editar `app.routes.ts` |
| La plataforma de dev se levanta y se verifica con `smoke.sh` | ❌ hasta corregir H2 |
| Ejemplos reales para los briefs | ✅ Pruebas de repositorio con Testcontainers por servicio, `FsTabla` en `/_ui`, `CONTADOR_MENU`, guards y `SIN_TOAST` |
| `implementados.txt` | ⚠️ H3: decidir quién agrega cada servicio |

## 7. Hallazgos

### Bloqueantes

**H1 — Rutas del backoffice: varias pantallas reciben la misma ruta vacía.**

Varias entradas del *shell* consumen la URL completa y cargan la misma carpeta, así que la carpeta recibe la ruta vacía `''` y no puede saber qué pantalla mostrar. Comprobado con una prueba del router: el resultado es la subruta que recibe la carpeta.

| URL | Subruta que recibe la carpeta | Problema |
|---|---|---|
| `/staff` y `/clientes` (carpeta `usuarios`) | `''` y `''` | spec 1 no puede distinguirlas |
| `/pagos` y `/reembolsos` (`pagos`) | `''` y `''` | spec 5 no puede distinguirlas |
| `/tarifas` y `/agencias` (`maestros-envio`) | `''` y `''` | spec 6 no puede distinguirlas |
| `/entregas`, `/entregas/dia` y `/repartidores` (`entregas`) | `''`, `''` y `''` | la SPEC-6 pide declarar `dia` en `entregas.routes.ts`, pero esa subruta nunca llegaría |
| `/catalogo/categorias`, `/maestros`, `/descuentos` y `/productos/carga-masiva` (`catalogo`) | `''` | spec 2 no puede distinguirlas |

**Corrección:** usar el patrón que ya sigue el storefront.

- Cada carpeta exporta un arreglo de rutas **por pantalla o grupo de R2**: `STAFF_ROUTES`, `CLIENTES_ROUTES`, `PAGOS_ROUTES`, `REEMBOLSOS_ROUTES`, `TARIFAS_ROUTES`, `AGENCIAS_ROUTES`, `ENTREGAS_ROUTES` (con `''`, `dia` y `:id`), `REPARTIDORES_ROUTES`, `PRODUCTOS_ROUTES`, `CATEGORIAS_ROUTES`, etc.
- El *shell* carga cada uno, y las guardas por rol se quedan en el *shell* (CA-0b-17).
- Nueva prueba de rutas: cada URL de R2 llega a la subruta de su pantalla (por ejemplo, `/entregas/dia` → `dia`).
- Cada carpeta conserva su marcador "Próximamente", con el título de cada subruta.

**H2 — `smoke.sh` no carga `.env` (regresión del PR #28).**

`infra/.env.example` tiene `MAIL_FROM=FitStore <no-responder@fitstore.local>`, que no es válido en bash. El `. infra/.env` del *smoke* se corta en esa línea, así que `CONFIG_USER`, `CONFIG_PASSWORD` y todas las variables siguientes quedan sin definir. Docker Compose lee bien ese archivo, por eso la plataforma arranca y solo fallan las verificaciones. Se reproduce siempre que `infra/.env` se cree desde el ejemplo, que es justo lo que hace `stage.yml`.

**Corrección:** `MAIL_FROM="FitStore <no-responder@fitstore.local>"`. Compose quita las comillas, así que el servicio recibe el mismo valor. Además, `smoke.sh` debe fallar de inmediato si el `.env` no se puede cargar, en vez de seguir sin variables.

### Importantes

**H3 — Nadie es dueño de `contracts/implementados.txt`.**

El archivo dice "cada spec agrega aquí su servicio al terminar", pero `CLAUDE.md` prohíbe a los agentes tocar `contracts/`, y la sección 8 de las specs 1–7 no lo lista.

**Propuesta:** lo agrega el **lead** en el PR del agente, al verificar V5 con `scripts/contrato.sh`. Se aclaran el comentario del archivo, `CLAUDE.md` y la estrategia (V5).

**H4 — Imágenes de Testcontainers sin fijar en 4 módulos.**

| Módulo | Imágenes |
|---|---|
| `fitstore-common` | `postgres:17` y `rabbitmq:4-management` |
| `auth`, `catalog`, `settings` | `postgres:17` |
| `api-gateway` | `redis:8` |

order, payment, delivery y notification usan las etiquetas del compose (`postgres:17.10`, `redis:8.10.1`). Una etiqueta flotante puede cambiar entre corridas, y Lyanna y el CI descargan dos imágenes de PostgreSQL.

**Corrección:** usar las etiquetas exactas del compose en todos, y en stack-versiones cambiar "Redis en pruebas: `redis:8`" por `redis:8.10.1`. Toca `fitstore-common` y el gateway, que están congelados, así que va en este mismo PR de corrección, con tu aprobación.

### Menores

**H5 — `UbigeoCatalogo` guarda en `localStorage` cada búsqueda de distrito, para siempre.**

Se guarda cada texto tecleado: "mi", "mir", "mira"… Además, el selector de la tienda busca en cada tecla, sin espera.

**Corrección:** persistir solo las listas (departamentos, provincias, distritos) y dejar las búsquedas en memoria. Esperar 300 ms antes de buscar. Al corregirlo se vio que el selector del backoffice tampoco esperaba (solo cancelaba la búsqueda anterior), así que se corrigen los dos.

**H6 — Filas `aviso`/`error` de `FsTabla`.**

`cursor: inherit` les quita la mano en las tablas con detalle. Además, sobre la fila amarilla, la insignia amarilla ("Pendiente de pago") se confunde con el fondo.

**Corrección:** mantener `cursor: pointer` en las filas clicables y marcar la fila con un contorno izquierdo de color en vez de rellenarla (decisión del autor).

**H7 — `CLAUDE.md` dice "la `V1` es de 0a".**

0a creó también `V2` y `V3` en catalog, settings, delivery y notification (semillas). El nombre con fecha (`VAAAAMMDD_HHMM`) igual queda después, pero se corrige el texto: "las `V1`–`V3` son de 0a".

## 8. Verificación de H2

Con el valor entre comillas en un clon limpio:

✅ **SMOKE OK**: 33 verificaciones pasaron, con todo *healthy* en 133 s. Las 7 verificaciones de `ddl-auto=validate` con `/actuator/health` UP ahora pasan. H2 era la única causa de la falla.

## 9. Observaciones (no bloquean)

- **O1 — Memoria:** en reposo, los 7 servicios están en ≈ 320 de 320 MiB (incluye la caché de páginas del contenedor) y el gateway en 244 de 256 MiB. Sin reinicios ni OOM en el *smoke*. Se mide con carga en la spec 8; hay margen de 1,7 GB dentro de los 6,5 GB para subir los `mem_limit`.
- **O2 — Etiqueta "Admin" fija en el menú lateral:** así está en el diseño (`BoBarra`), aunque un OPERADOR también la ve. Se mantiene como en el diseño.
- **O3 — Storefront:** las carpetas `catalogo`, `acceso` y `legal` cuelgan de la ruta `''`. Al abrir `/ingresar`, el router descarga también el chunk de `catalogo` para probar coincidencias. Es un detalle de rendimiento, sin efecto funcional; se revisa con Lighthouse en la spec 8.
- **O4 — Selector de ubigeo con API distinta por app:** la tienda usa `valor` y `(cambio)`; el backoffice, `[(valor)]`. Cada spec usa el de su app, y los briefs lo indican.
- **O5 — `settings-service`:** su `application.yml` usa el perfil `pruebas` (los otros 6 usan `sin-config-server`), `${CONFIG_USER:}` con valor vacío por defecto y 10 reintentos (los demás, 20). Funciona; se le indica a la SPEC-3, que es dueña del servicio, que lo alinee.
- **O6 — Pendientes de CK1 y 0b:**
  - los colores derivados se desvían hasta 10 por canal del diseño en marcas que no son la plantilla;
  - `package-lock.json` generado con npm 11;
  - la tienda llama a `/auth/refresh` en cada arranque, también sin sesión (401 silencioso, por diseño de CA-0b-12).

## 10. Correcciones

Aprobadas por el autor el 2026-10-20.


| PR | Contenido |
|---|---|
| `fix/ck2-base` | H1 (rutas por pantalla con su prueba), H2 (comillas y `smoke.sh` estricto), H4 (etiquetas exactas), H5 y H6 (cursor) |
| `docs/ck2` | Este informe; H3 (`implementados.txt`, `CLAUDE.md`, estrategia V5); H7; stack-versiones v9 (`redis:8.10.1`); decisiones de 0b (etiquetas de insignias, logo claro u oscuro, `/_ui` con `ngDevMode`, *shell* diferido, la barra de anuncio sin dato en settings); tablero |
