# 21. Flujo de ramas dev → stage → main con CI/CD por etapa

Date: 2026-10-18

## Status

Accepted (2026-10-18) — sin políticas de rama forzadas por GitHub

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
| push a `main` | `deploy.yml` | Publica las imágenes en GitHub Container Registry y despliega en la VM por SSH (`docker compose pull && up -d`). El PR de `stage` a `main` es la aprobación; también se puede lanzar a mano (`workflow_dispatch`). Se activa con la spec 8 |

**Políticas: por convención, no forzadas**

- Nunca se hace commit ni push directo a `main` ni a `stage`; todo entra por PR.
- Los PR usan una plantilla con la lista de verificación y se integran solo con los checks en verde.
- Las reglas de rama de GitHub (*rulesets*) y los entornos con aprobación **no se configuran**: en repositorios privados del plan Free requieren GitHub Pro, y el autor decidió no activarlo (era deseable, no obligatorio). Tampoco se agrega un *hook* local.

## Alternativas

- Hacer público el repositorio (habilita los rulesets gratis): descartado mientras dure el curso, por probidad académica.
- Solo `dev` y `main`: menos ceremonia, pero sin una etapa donde integrar todo el sistema antes de la demo.

## Consecuencias

- Toda la documentación y el código entran por PR a `dev` desde 2026-10-18; `dev` es la rama por defecto del repositorio.
- La SPEC-0a cambia: crea `ci.yml`, `stage.yml` y la plantilla de PR; `deploy.yml` lo crea la spec 8.
- La disciplina de ramas depende del autor y de las instrucciones a los agentes (estrategia multiagente).
