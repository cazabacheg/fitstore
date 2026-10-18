# 21. Flujo de ramas dev → stage → main con CI/CD por etapa

Date: 2026-10-18

## Status

Proposed

## Contexto

Hasta la aprobación de la SPEC-2 todo se trabajó directamente en `main`. Con la implementación multiagente se necesitan etapas de integración, validación y despliegue, y que nadie (ni los agentes) pueda escribir directamente en `main`.

## Decisión

**Ramas**

| Rama | Rol | Entra por |
|---|---|---|
| `main` | Producción (la demo en la nube) | PR desde `stage` (o `hotfix/*`) |
| `stage` | Preproducción: integración completa del sistema | PR desde `dev` |
| `dev` | Integración diaria | PR desde ramas de trabajo |
| `spec/NN-…`, `fix/…`, `docs/…` | Trabajo (una por spec o por agente) | Se crean desde `dev` |
| `hotfix/…` | Corrección urgente de producción | Se crea desde `main`; luego se integra de vuelta en `stage` y `dev` |

**Estrategia de merge:** ramas de trabajo → `dev` con *squash* (un commit por cambio); `dev` → `stage` → `main` con *merge commit* (las promociones conservan los mismos commits, sin divergencias).

**CI/CD por etapa**

| Disparador | Workflow | Qué hace |
|---|---|---|
| PR a `dev` / push a `dev` | `ci.yml` | Compilación y pruebas (unitarias y de datos con Testcontainers), lint y pruebas del frontend, gitleaks, verificación de clientes de API generados. Filtros por ruta y caché |
| PR a `stage` / push a `stage` | `stage.yml` | Además: construye las imágenes Docker, levanta el compose completo **en el runner** (entorno efímero, sin costo), corre `smoke.sh`, las colecciones Postman (Newman) y las pruebas E2E (desde la spec 8) |
| PR a `main` | `stage.yml` | Las mismas verificaciones de `stage` |
| push a `main` | `deploy.yml` | Publica las imágenes en GitHub Container Registry y despliega en la VM por SSH (`docker compose pull && up -d`), con aprobación manual del entorno `produccion`. Se activa con la spec 8 |

**Políticas (rulesets de GitHub)**

| Rama | Reglas |
|---|---|
| `main`, `stage` | PR obligatorio, checks obligatorios en verde, sin *force push*, sin borrado, sin bypass |
| `dev` | PR obligatorio, check `ci` obligatorio, sin *force push* |

Al ser un desarrollador único, los PR no exigen aprobación de otra persona (no hay quién apruebe), pero sí PR y checks; cada PR usa una plantilla con la lista de verificación.

## Requisito

Los *rulesets* y los entornos con aprobación **no están disponibles en repositorios privados del plan Free** (la API responde "Upgrade to GitHub Pro"). Se resuelve activando **GitHub Pro gratis con el GitHub Student Developer Pack**.

**Mientras no esté activo:** un *hook* local `pre-push` versionado (`.githooks/`, `core.hooksPath`) que impide hacer push directo a `main` y `stage`, y un workflow que avisa si llega un push directo. Es una barrera de disciplina, no de seguridad.

## Alternativas

- Hacer público el repositorio (habilita los rulesets gratis): descartado mientras dure el curso, por probidad académica.
- Solo `dev` y `main`: menos ceremonia, pero sin una etapa donde integrar todo el sistema antes de la demo.

## Consecuencias

- Toda la documentación y el código entran por PR a `dev` desde 2026-10-18.
- La SPEC-0a cambia: su CI pasa a ser tres workflows (`ci.yml`, `stage.yml`, `deploy.yml`) y crea `.githooks/` y la plantilla de PR.
