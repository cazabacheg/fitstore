# FitStore — Estrategia multiagente de implementación

> Versión: **v7 (aprobado)** — 2026-10-20 · Estados: ✅ decidido · 🟡 propuesto · ❓ abierto
> Etapa: specs ✅ → contratos ✅ → diseño ✅ → **estrategia multiagente** → implementación.

**Historial:**
- v7 (2026-10-20): por CK2, el lead agrega el servicio a `contracts/implementados.txt` al verificar V5 (los agentes no tocan `contracts/`).
- v6 (2026-10-19): **validación del brief contra la spec** antes de lanzar cada agente (B2), tras detectar en 0a.1 criterios asignados al paso equivocado.
- v5 (2026-10-19): **spec primero** en toda validación (V-c); **terminado = 100 % del alcance de la spec**, sin agregados ni adelantos (V0b, V11). La cobertura de 80 % es un piso de calidad, no la medida de terminado.
- v4 (2026-10-19): **checkpoints de base** (sección 2.1): revisión exhaustiva del lead antes de arrancar agentes en paralelo (tras 0a.1 y tras 0a + 0b).
- v3 (2026-10-19): cobertura mínima 80 % con el núcleo cubierto (V10); rojo por historia confirmado (F3); la ola 0 se parte en 0a.1 → 0a.2 (los 7 esqueletos, 3 a la vez) → 0a.3 → **estrategia aprobada**.
- v2 (2026-10-19): respuestas del autor: 3 agentes a la vez (O1 ✅), el lead integra tras la aprobación (C1 ✅), tablero en archivo ✅, pantallas revisadas en el navegador o en el chat (V6 ✅). Nuevo: los implementadores leen la spec **y el micro** de su servicio (B1), corren con **Sonnet** (M1), y TDD *test first* como barrera durante toda la construcción (F2–F4, V3).
- v1 (2026-10-19): borrador inicial: roles (autor, lead, implementadores), olas, flujo TDD por agente, verificación del DoD por el lead, puntos de control y herramientas.

Cómo se implementan las 10 specs con varios agentes de IA en paralelo sin pisarse y sin dar por terminado algo que no lo está. Se apoya en lo ya decidido: corte por servicio y reglas de convivencia ([`specs/README.md`](../specs/README.md)), flujo de ramas ([ADR-021](../adr/0021-flujo-de-ramas-dev-stage-main.md)), contratos ([`contracts/`](../../contracts/README.md)) y TDD como barrera (K13).

## 1. Roles

| Rol | Quién | Responsabilidad | No hace |
|---|---|---|---|
| **Autor** | Carlos | Aprueba cada ola y cada PR antes de integrarlo, decide cambios de spec o de contrato, hace la demostración manual de cada DoD y marca "Revisado y aprobado por el autor" | Escribir el código de las specs (puede, pero no es el flujo normal) |
| **Lead (orquestador)** | La sesión principal de Claude Code | Planifica las olas, prepara el *brief* de cada agente, lanza y sigue a los implementadores, responde sus dudas dentro de la spec, **verifica de forma independiente la definición de terminado** de cada uno, abre el PR y mantiene el tablero | Implementar specs él mismo; aprobar sus propias excepciones; marcar la casilla del autor |
| **Implementador** | Un subagente por spec, en su propio *worktree*, con el modelo **Sonnet** (M1) | Implementa su spec con TDD dentro de sus carpetas, deja evidencia de cada criterio y se detiene a preguntar cuando algo no está en la spec | Tocar carpetas ajenas o congeladas, cambiar contratos, integrar a `dev` |

**M1 ✅ Modelos:** el lead corre con Opus; los implementadores, con **Sonnet**. Por eso el brief es explícito (rutas exactas, qué leer y en qué orden, historia por historia) y la verificación del lead no se relaja.

**L1 ✅ El lead no confía en el reporte del implementador:** vuelve a correr todo por su cuenta y revisa el diff. Un "listo" del implementador es una solicitud de verificación, no una entrega.

## 2. Olas

Las dependencias salen de la tabla de [`specs/README.md`](../specs/README.md): 0a y 0b son la base, las specs 1 a 7 son independientes entre sí (se hablan por contratos y WireMock) y la 8 integra todo.

| Ola | Specs | Paralelismo | Entra cuando |
|---|---|---|---|
| 0 | **0a** Plataforma backend, en tres pasos (SPEC-0a I1): **0a.1** monorepo, `fitstore-common` e infraestructura → **0a.2** los 7 esqueletos → **0a.3** CI, *smoke* y Postman | 0a.1 y 0a.3: 1 agente. 0a.2: un agente por esqueleto, **3 a la vez** (no comparten archivos) | 0a.1 ahora; 0a.2 cuando 0a.1 esté en `dev`; 0a.3 cuando estén los 7 |
| 1 | **0b** Fundaciones del frontend | 1 agente | 0a integrada en `dev` |
| 2a | **1** Identidad, **2** Catálogo, **3** Tienda y reclamos | 3 agentes | 0b integrada |
| 2b | **4** Pedidos, **5** Pagos (empieza por la PoC de pasarelas, P1), **6** Envíos, **7** Notificaciones | 3 agentes a la vez; el cuarto entra cuando sale uno de 2a | Cada una, cuando haya un hueco |
| 3 | **8** Integración, nube y demo | 1 agente | 1 a 7 integradas; `dev → stage` en verde |

### 2.1 Checkpoints de base

Antes de arrancar agentes en paralelo sobre una base, el lead la revisa **completa** (no por muestra): un error en la base se multiplica en cada agente que construye encima.

| Checkpoint | Cuándo | Qué protege |
|---|---|---|
| **CK1** | 0a.1 integrada, antes de lanzar los 7 esqueletos (0a.2) | Monorepo, `fitstore-common`, gateway, config, compose y la base mínima de los 7 servicios |
| **CK2** | 0a y 0b integradas, antes de lanzar la ola 2 (specs 1–7) | Todo lo anterior más los esqueletos, el CI, el *smoke*, el workspace Angular, `shared`, `ui/`, layouts y el tema |

**Qué revisa el lead en cada checkpoint:**

1. **Desde cero:** clona `dev` en una carpeta limpia, `./mvnw verify` (y `npm ci && npm test` en CK2) y levanta el compose de dev; todo en verde y *healthy*.
2. **Código completo de la base, archivo por archivo**, contra **la spec primero** (SPEC-0a en CK1; además SPEC-0b en CK2) **y luego el micro** (infraestructura 2–10): paquetes y nombres como en 10.3, nada de lógica de negocio en `fitstore-common`, cada filtro del gateway en el orden de 10.1.
3. **Contratos:** `ApiResponse`, `ApiError`, página y sobre de eventos idénticos a K5, K6 y K12 (los usarán todos los servicios).
4. **Pruebas y cobertura:** informe completo de JaCoCo (y Vitest en CK2), todo el núcleo cubierto; lectura de todas las pruebas de la base, no de una muestra.
5. **Seguridad:** sin secretos versionados, la clave JWKS de prueba marcada como tal, todo cerrado salvo `health` y la documentación (I3).
6. **Lista para el paralelo:** cada agente siguiente puede trabajar **solo en su carpeta** (ningún archivo compartido por completar), y su brief puede apuntar a ejemplos reales de la base (cómo se escribe una prueba de datos, un consumidor, una configuración).
7. **Briefs siguientes:** se completan con lo aprendido (rutas reales, ejemplos, trampas encontradas).

**Resultado:** un informe al autor con lo revisado, los hallazgos y su corrección. Los hallazgos se corrigen **antes** del paralelo (PR `fix/…` o una ronda más del agente de la base). El autor aprueba el checkpoint y recién ahí se lanzan los agentes en paralelo.

- **O1 ✅ Máximo 3 implementadores a la vez, cuando las dependencias lo permitan.** Límites reales: tu capacidad de revisar PRs, la RAM de Lyanna (cada agente levanta PostgreSQL, Redis y RabbitMQ con Testcontainers) y los minutos de GitHub Actions. Con 3 se terminan 1–7 en unas 3 vueltas.
- **O2 🟡 1 y 2 primero dentro de 2a:** auth (JWT real) y catalog (productos) son las que más usan las demás en sus pruebas manuales; el resto trabaja contra WireMock de todas formas.
- **O3 🟡 Un agente por spec, backend y luego frontend.** Partir una spec en dos agentes (backend y frontend en paralelo) duplica la coordinación por poca ganancia; el frontend usa los clientes generados del contrato, así que no espera al backend real.

## 3. Brief de cada implementador

El lead arma un *brief* con esta plantilla y lo guarda en `docs/implementacion/briefs/SPEC-NN.md` (queda registrado qué se le pidió a cada agente):

1. **Lectura obligatoria antes de escribir código (B1 ✅)**, en este orden: la **spec** completa (historias, criterios, decisiones, sección 7 "CA → prueba", sección 8 "archivos que toca", sección 9 DoD) y el **micro** de su servicio en [`docs/analisis/micro/`](../analisis/micro/) (modelo, invariantes, endpoints, eventos, diseño técnico). Si un dato no está en ninguno de los dos ni en los contratos, no se inventa: se pregunta (sección 5). El agente confirma en su primer mensaje qué leyó y resume en 5–10 líneas lo que va a construir; el lead lo contrasta con la spec antes de dejarlo seguir.
2. **Contratos** que implementa (OpenAPI de su servicio, eventos del AsyncAPI que publica y consume) y los que consume de otros (solo como mocks).
3. **Diseño:** pantallas de [`docs/diseno/`](../diseno/) y componentes de `ui/` que usa.
4. **Carpetas propias** (copiadas de su sección 8) y la lista de congelados.
5. **Rama y worktree:** `spec/NN-nombre` desde `dev`.
6. **Cuándo detenerse y preguntar** (sección 5).
7. **Cómo reportar** (sección 4.3).

**B2 ✅ Validación del brief antes de lanzar (spec primero).** El brief no se arma "de memoria" ni por inferencia: cada asignación se copia de la spec. Antes de lanzar a un agente, el lead completa en el brief la tabla **"Validación del brief"** con todos los criterios de la spec y comprueba:

1. **Cada criterio** de la spec aparece **exactamente una vez** como dueño (un paso o un agente), sin huecos ni duplicados. Si otro paso agrega una prueba complementaria, se anota como tal, sin quitarle el criterio a su dueño.
2. **Dónde se prueba** coincide literalmente con la fila de la **sección 7** de la spec (tipo y archivo). Si la sección 7 dice *smoke* o CI, el criterio es de quien crea ese *smoke* o ese CI.
3. **El dueño puede probarlo:** el archivo de prueba que indica la sección 7 está dentro del alcance (`.alcance`) de ese paso.
4. **El alcance** de cada paso está contenido en la **sección 8** de la spec, y la unión de los pasos la cubre completa.
5. **"Incluye" y "No incluye"** de la sección 1 están repartidos sin huecos y sin adelantar trabajo de otras specs.

El autor ve esa tabla al aprobar la ola. Si el agente encuentra después una asignación mal hecha, el lead corrige el brief por PR y registra el error en el tablero.

Las reglas comunes a todos los agentes (convenciones, TDD, commits, carpetas congeladas) van en un `CLAUDE.md` en la raíz del repo, que Claude Code carga en cada agente; el brief solo agrega lo propio de la spec.

## 4. Flujo de un implementador

**F2 ✅ Spec-driven con TDD *test first* como barrera:** la spec dice qué construir; las pruebas, escritas antes que el código, mantienen la construcción en carril. Ninguna línea de código de producción se escribe sin una prueba en rojo que la pida.

### 4.1 Ciclo por historia

1. Lee la historia, sus criterios y lo que dice el micro sobre ellos.
2. **Rojo:** escribe las pruebas de esos criterios según la tabla de la sección 7; cada prueba **lleva el id del criterio** en su nombre (`@DisplayName("CA-2-14 aplica el tramo alcanzado")`, `it('CA-2-14 …')`). Corre y confirma que fallan por la razón esperada. Commit `test(spec-2): CA-2-12 a CA-2-15 en rojo`.
3. **Verde:** implementa lo mínimo para que pasen. Commit `feat(spec-2): …`.
4. **Toda la suite sigue en verde:** corre todas las pruebas del servicio (y del proyecto de frontend), no solo las nuevas. Lo construido en historias anteriores no se puede romper.
5. **Refactor** con las pruebas en verde. Commit si aplica.
6. Pasa a la siguiente historia.

- **F3 ✅ Rojo por historia, no de toda la spec de golpe:** con 30 a 60 criterios por spec, escribir todo en rojo al inicio deja cientos de pruebas que ni compilan y le quita foco al agente. Por historia, el rojo es chico y verificable, y la suite completa crece en verde.
- **F4 ✅ Las pruebas no se tocan para que pasen:** está prohibido borrar, deshabilitar (`@Disabled`, `xit`, `.skip`) o debilitar una prueba (quitar aserciones, ampliar tolerancias). Si una prueba parece estar mal, el agente se detiene y pregunta (sección 5).

**F1 ✅ Commits de prueba antes que los de código** (los PR se integran con *squash*, pero la historia de la rama queda en el PR como evidencia del rojo → verde).

### 4.2 Antes de pedir verificación

- Backend: `./mvnw -pl <servicio> -am verify` (unitarias, de datos con Testcontainers, de integración).
- Frontend: `npm run lint`, `npm test -- --watch=false` del proyecto que tocó, y la verificación de clientes generados.
- Contrato: `oasdiff` del OpenAPI de su servicio contra lo implementado sin cambios incompatibles (K2).
- Postman de su servicio con el compose local, si la spec lo pide.

### 4.3 Reporte al lead

Un mensaje con: rama y último commit; tabla **CA → archivo de prueba** completa; comandos corridos y su resultado; lo que **no** pudo cumplir y por qué; preguntas abiertas. Sin reporte completo, el lead no empieza la verificación.

## 5. Cuándo un implementador se detiene y pregunta

| Situación | Qué hace | Quién decide |
|---|---|---|
| La spec no dice qué hacer o se contradice | Se detiene y pregunta al lead | Lead si la respuesta ya está en specs, micros, contratos o diseño; si no, el autor |
| Necesita cambiar un contrato | Se detiene | **Autor.** Si se aprueba: PR aparte de `contracts/` y de la spec (con barrido de documentos), y luego el agente sigue sobre `dev` actualizado |
| Necesita tocar una carpeta congelada o ajena | Se detiene | **Autor.** Si se aprueba: PR aparte, uno a la vez (regla 2 de convivencia) |
| Una prueba no se puede escribir como dice la sección 7 | Propone la alternativa | Lead (si cubre el mismo criterio); autor si cambia el criterio |
| Una dependencia o versión no está en [`stack-versiones`](../arquitectura/stack-versiones.md) | Se detiene | Autor |
| Una prueba parece estar mal o contradice la spec | Se detiene; no la cambia | Lead si la spec es clara; si no, el autor |
| Dos intentos fallidos con el mismo error | Se detiene y reporta lo intentado | Lead |

**P1 ✅ Ningún agente cambia una spec, un contrato o un documento aprobado por su cuenta.**

## 6. Verificación de la definición de terminado (la hace el lead)

El lead pasa esta lista **en el worktree del agente, con sus propios comandos**, antes de abrir el PR a `dev`:

**V-c ✅ Spec primero.** Toda validación del lead (verificaciones V1–V11, checkpoints, respuestas a dudas) se contrasta **primero contra la spec y luego contra el micro**. Si difieren, manda la spec (es la decisión más reciente y aprobada) y el lead anota la diferencia para corregir el micro en un PR de documentación, con barrido.

**V0b ✅ Terminado = 100 % del alcance de la spec.** Un agente está terminado solo cuando cumple **todo** lo que su spec define (o el paso que le tocó del brief): cada criterio de aceptación, cada punto de "Incluye" de la sección 1, cada archivo de la sección 8 y cada casilla de la sección 9 que no sea del autor. Y **nada más**: ni funciones de otras specs, ni adelantos de lo que viene, ni extras "por si acaso". Lo que la spec deja fuera ("No incluye") no se construye.

**V0 ✅ Cobertura: 80 % mínimo, con todo lo esencial cubierto.** Es un piso de calidad de las pruebas, no la medida de terminado (esa es V0b). No se persigue el 100 %: el umbral de 80 % de líneas por módulo lo hace cumplir el CI (SPEC-0a, SPEC-0b), y además el **núcleo** debe estar cubierto por completo. Núcleo = lo que decide el negocio: entidades con reglas e invariantes, servicios de aplicación (casos de uso), máquinas de estado, calculadoras (descuentos, montos, tarifas), consumidores y publicadores de eventos, adaptadores de pasarela y de almacenamiento, filtros de seguridad del gateway. Cada spec lo hace explícito en su sección 7 y el brief lo lista.


| # | Verificación | Cómo |
|---|---|---|
| V1 | **Todos los criterios tienen prueba** | Script `scripts/ca-cobertura.sh SPEC-NN`: extrae los `CA-NN-xx` de la spec y los busca en los nombres de las pruebas; la lista de faltantes debe estar vacía |
| V2 | **Las pruebas pasan** | El lead corre de nuevo los comandos de 4.2 (no copia la salida del agente) |
| V3 | **Las pruebas prueban algo y nadie las aflojó** | Revisa una muestra de criterios por historia: que la prueba verifique el resultado del criterio (no solo que "no falle") y que exista el commit en rojo antes del de código. Busca pruebas borradas, deshabilitadas o con aserciones quitadas (`git diff dev...` sobre los archivos de prueba, `@Disabled`, `xit`, `.skip`) |
| V4 | **Solo tocó lo suyo** | `git diff --name-only dev...` contra la sección 8 de la spec; cualquier archivo fuera de la lista es un rechazo |
| V5 | **Contratos respetados** | `oasdiff` sin cambios incompatibles; los eventos publicados y consumidos coinciden con el AsyncAPI. El lead agrega el servicio a `contracts/implementados.txt` en el PR, para que el CI compare su contrato desde entonces |
| V6 | **Diseño respetado** (specs con pantallas) | Compara las pantallas clave con [`docs/diseno/`](../diseno/) con capturas de Playwright a 1280 y 390 px, y **te las muestra en el chat**; si prefieres, abres la app levantada en Lyanna en tu navegador |
| V7 | **Higiene** | gitleaks limpio, sin secretos en `application*.yml`, commits sin atribución de IA y con autor `cazabacheg`, migraciones con el nombre convenido |
| V8 | **CI en verde** | `ci.yml` del PR a `dev` |
| V11 | **Alcance exacto (V0b)** | El lead recorre la spec sección por sección (1 "Incluye", 3 criterios, 5 decisiones, 6 datos, 8 archivos) y marca cada punto como hecho con su evidencia; cualquier punto faltante es un rechazo. Revisa también el diff en busca de lo que **sobra** (endpoints, pantallas, tablas o dependencias que la spec no pide, o que pertenecen a otra spec): se quita |
| V10 | **Cobertura** | Informe de JaCoCo (backend) y de Vitest (frontend): **al menos 80 % de líneas** en cada módulo que tocó (sin contar configuración, DTOs, clases `Application` ni código generado), y el **núcleo cubierto**: el lead abre el informe del núcleo y cada línea o rama sin cubrir debe estar justificada en el PR |
| V9 | **Resto de la sección 9** | Cada casilla del DoD de la spec, salvo la del autor; las que requieren `stage` (Postman completo, k6) se marcan pendientes de la promoción |

- **V-a ✅ Si algo falla,** el lead devuelve al agente la lista concreta de lo que no se cumple. Máximo **2 rondas**; a la tercera, el lead se lo plantea al autor con el diagnóstico.
- **V-b ✅ El PR a `dev`** lleva la lista V1–V11 marcada con la evidencia (salida de los comandos, enlaces). El autor revisa, hace la demostración de la sección 9 y aprueba; recién ahí se integra.

## 7. Puntos de control del autor

| Momento | El autor |
|---|---|
| Inicio de cada ola | Aprueba qué specs entran y sus briefs |
| Checkpoints de base CK1 y CK2 (2.1) | Revisa el informe del lead y aprueba antes de lanzar agentes en paralelo |
| Preguntas escaladas (sección 5) | Decide |
| Cada PR a `dev` | Revisa, demuestra la sección 9 y aprueba la integración |
| Promoción `dev → stage` | Al cerrar cada ola: `stage.yml` corre el compose completo, Postman y (desde la 8) E2E |
| Promoción `stage → main` | Solo con la spec 8: es la aprobación del despliegue |

**C1 ✅ Quién integra:** el lead integra el PR a `dev` (*squash*) solo después del "aprobado" explícito del autor en la conversación. Las promociones a `stage` y `main` también las pide el autor.

## 8. Herramientas

- **Lead:** la sesión principal de Claude Code en Lyanna, en `~/CIBERTEC/PROYECTOS FINALES/fitstore`.
- **Implementadores:** subagentes de Claude Code lanzados por el lead con *worktree* aislado (cada uno en su copia del repo y su rama, sin pisar al resto) y en segundo plano; el lead les escribe para responder dudas o devolver observaciones.
- **`CLAUDE.md` en la raíz** con las reglas comunes (sección 3).
- **Scripts del lead** en `scripts/`: `ca-cobertura.sh` (V1) y `fuera-de-alcance.sh` (V4). Se crean en la ola 0 junto con 0a.
- **Tablero ✅:** `docs/implementacion/tablero.md`, que el lead actualiza en cada cambio de estado: spec, agente, rama, estado (brief, en curso, en verificación, en revisión del autor, integrada), ronda de verificación, PR y bloqueos. Es simple y queda versionado; los PR de GitHub dan el detalle.

## 9. Riesgos

| Riesgo | Mitigación |
|---|---|
| El agente da por hecho algo que no hizo | L1 y V1–V9: el lead verifica con sus propios comandos |
| Pruebas que pasan sin probar nada | V3 (muestra por historia y commit en rojo) |
| Un agente con Sonnet inventa lo que no está escrito | B1 (spec y micro antes de codificar, resumen contrastado por el lead) y sección 5 (preguntar en vez de suponer) |
| Conflictos entre ramas paralelas | Un dueño por carpeta, congelados y contratos fijos; cada rama se actualiza con `dev` antes de su PR |
| Lyanna se queda sin memoria con varios Testcontainers | O1 (máximo 3 a la vez); el lead escalona las corridas pesadas |
| Cambios de contrato a mitad de una ola | Sección 5: PR aparte aprobado por el autor y barrido de documentos antes de seguir |
| Se agotan los minutos de Actions | Filtros por ruta de `ci.yml`; `stage.yml` solo al cerrar cada ola |

## 10. Preguntas abiertas

Ninguna. Respondidas en v2: 3 agentes a la vez (O1), el lead integra tras tu aprobación (C1), tablero en archivo (sección 8), diseño revisado en el navegador o en el chat (V6).
