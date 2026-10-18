# 18. Monorepo, build y CI con GitHub Actions

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** monorepo `fitstore` en GitHub (privado, `cazabacheg`): Maven multi-módulo para backend (con módulo `common`), workspace Angular con `storefront`, `backoffice` y librería `shared`, carpeta `infra` con docker-compose por entorno.
- **CI con GitHub Actions:** build + pruebas en cada push/PR, con filtros por ruta (solo se compila lo que cambió) y caché de Maven/npm. Plan gratuito: 2 000 min/mes en repos privados (más con GitHub Pro del Student Pack); suficiente para este proyecto. CD opcional: despliegue por SSH a la VM.
- **Alternativa:** Azure Pipelines (1 800 min/mes gratis en proyectos privados, pero requiere Azure DevOps aparte y solicitar la concesión gratuita; agrega una plataforma más sin beneficio, ya que el código vive en GitHub).

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
