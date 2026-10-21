# FitStore — Reglas para los agentes

E-commerce white-label para MYPEs de ropa deportiva de Gamarra: 10 servicios Spring Boot 4 + 2 frontends Angular 22. Proyecto final de Cibertec (DAW II y DSW II). Todo se escribe en **español de Perú**: código de dominio, comentarios, mensajes, commits y documentación.

La forma de trabajo completa está en [`docs/implementacion/estrategia-multiagente.md`](docs/implementacion/estrategia-multiagente.md). Este archivo resume lo que todo agente debe cumplir.

## 1. Antes de escribir código: leer, no suponer

1. Tu **brief** (`docs/implementacion/briefs/`), que dice qué spec implementas y qué carpetas son tuyas.
2. La **spec** completa (`docs/specs/SPEC-NN-*.md`).
3. El **micro** de tu servicio (`docs/analisis/micro/<servicio>.md`); para 0a, también `infraestructura.md`.
4. Los **contratos** que implementas o consumes (`contracts/openapi/`, `contracts/asyncapi/fitstore-eventos.yaml`).
5. Si tienes pantallas: el **diseño** (`docs/diseno/`) y los componentes de `ui/`.
6. Las versiones de [`docs/arquitectura/stack-versiones.md`](docs/arquitectura/stack-versiones.md). No agregues dependencias ni cambies versiones que no estén ahí.

**La spec manda:** léela primero y, si algo del micro la contradice, sigue la spec y avisa al lead. Si un dato no está en la spec, el micro, los contratos ni el diseño, **no lo inventes: detente y pregunta al lead.** En tu primer mensaje confirma qué leíste y resume en 5–10 líneas lo que vas a construir.

## 2. TDD *test first*: las pruebas son la barrera

Por cada historia de usuario, en orden:

1. **Rojo:** escribe las pruebas de sus criterios (tabla de la sección 7 de la spec). Córrelas y confirma que fallan por la razón esperada. Commit `test(spec-NN): CA-NN-xx a CA-NN-yy en rojo`.
2. **Verde:** escribe lo mínimo para que pasen. Commit `feat(spec-NN): …`.
3. **Toda la suite en verde:** corre todas las pruebas del módulo, no solo las nuevas. Nada de lo anterior se puede romper.
4. **Refactor** con todo en verde. Commit `refactor(spec-NN): …` si aplica.

Reglas que no se rompen:

- **Ningún código de producción sin una prueba en rojo que lo pida.**
- **Cada prueba lleva el id de su criterio** en el nombre: `@DisplayName("CA-2-14 aplica el tramo alcanzado")`, `it('CA-2-14 …')`. En scripts y workflows, un comentario `# CA-0a-02`. El lead lo verifica con `scripts/ca-cobertura.sh`.
- **Nunca** borres, deshabilites (`@Disabled`, `xit`, `.skip`, `fdescribe`) ni debilites una prueba (quitar aserciones, ampliar tolerancias) para que pase. Si una prueba parece estar mal, detente y pregunta.
- **Cobertura:** mínimo **80 % de líneas** por módulo (lo exige el build) y el **núcleo** de tu spec cubierto por completo (lo lista tu brief). No se persigue el 100 % con pruebas vacías.
- Las pruebas verifican el **resultado** del criterio (estado, respuesta, evento publicado), no solo que "no lanza error".

## 3. Alcance: todo lo de tu spec, nada más

- **Terminado = 100 % del alcance de tu spec** (o del paso de tu brief): cada criterio de aceptación, cada punto de "Incluye", cada archivo de la sección 8 y cada casilla de la sección 9 que no sea del autor. La cobertura de 80 % es un piso de calidad, no la medida de terminado.
- **Nada fuera de la spec:** ni funciones de otras specs, ni adelantos de lo que viene, ni extras "por si acaso". Lo que la spec pone en "No incluye" no se construye.

- Toca **solo** las rutas que tu brief y la sección 8 de tu spec te asignan. El lead lo verifica con `scripts/fuera-de-alcance.sh`.
- **Congelados** (no se tocan sin aprobación del autor y PR aparte): `backend/fitstore-common`, `backend/api-gateway`, `backend/config-server`, `backend/discovery-server`, `pom.xml` padre, `infra/config-repo/application.yml`, `infra/docker-compose*.yml`, `.github/workflows/`, `frontend/projects/shared`, `ui/`, `layout/`, rutas y menús generales, `scripts/contrato.sh`.
- **Nunca** cambies una spec, un micro, un contrato ni otro documento de `docs/` o `contracts/`. Si crees que hace falta, detente y pregunta.
- Migraciones nuevas: `VAAAAMMDD_HHMM__descripcion.sql` (las `V1`–`V3` son de 0a).
- `contracts/implementados.txt` no lo tocas: el lead agrega tu servicio en tu PR al verificar el contrato (V5).
- Clientes de API del frontend: siempre generados desde OpenAPI en `frontend/projects/shared/src/lib/api`; nunca a mano.
- Rutas del frontend: conserva el nombre del arreglo que importa el *shell* (`STAFF_ROUTES`, `PEDIDOS_ROUTES`…) y pon `data: { pantalla: '<id>' }` en cada ruta de tu carpeta, con el id que ya tenía el marcador. Las pruebas de rutas de 0b lo revisan.

## 4. Cuándo detenerte y preguntar al lead

- La spec no dice qué hacer, se contradice o choca con el micro o el contrato.
- Necesitas cambiar un contrato, tocar algo congelado o ajeno, o agregar una dependencia.
- Una prueba no se puede escribir como dice la sección 7, o parece estar mal.
- Dos intentos seguidos fallan con el mismo error.

Pregunta con: qué intentabas, qué dice la documentación (con ruta y sección), qué opciones ves.

## 5. Git

- Trabaja en tu rama `spec/NN-nombre` (o la que diga tu brief), creada desde `dev`. **Nunca** hagas commit ni push a `dev`, `stage` ni `main`, y nunca integres un PR: eso lo hace el lead tras la aprobación del autor.
- Autor de los commits: `cazabacheg <94200821+cazabacheg@users.noreply.github.com>`.
- **Sin atribución de IA** en commits, PRs ni archivos: nada de `Co-Authored-By`, `Claude-Session`, "Generated with Claude Code" ni similares.
- Nunca subas secretos: `.env` está en `.gitignore`; solo existe `.env.example`. Las claves de prueba de dev van marcadas como tales (SPEC-0a I2).

## 6. Antes de pedir verificación

Corre y guarda la salida de:

- Backend: `./mvnw -pl backend/<modulo> -am verify` (pruebas + JaCoCo).
- Frontend (desde 0b): `npm run lint` y `npm test -- --watch=false --coverage` del proyecto que tocaste.
- Contrato (desde que tu servicio tiene endpoints): `scripts/contrato.sh <servicio>`.
- `scripts/ca-cobertura.sh <spec>` sin faltantes.

Luego reporta al lead: rama y último commit, tabla **CA → archivo de prueba**, la lista de "Incluye" y de la sección 8 marcada punto por punto, comandos con su resultado, lo que no pudiste cumplir y por qué, y tus preguntas abiertas. "Listo" sin evidencia no es una entrega.
