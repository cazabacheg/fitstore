# FitStore — Tablero de implementación

> Lo actualiza el lead en cada cambio de estado ([estrategia multiagente](estrategia-multiagente.md), sección 8).
> Estados: ⏳ en espera · 📝 brief listo · 🔨 en curso · 🔍 en verificación (ronda N) · 👀 en revisión del autor · ✅ integrada · ⛔ bloqueada

## Ola en curso: 0 — SPEC-0a

| Paso | Agente | Rama | Estado | Ronda | PR | Bloqueos / notas |
|---|---|---|---|---|---|---|
| 0a.1 Monorepo, común e infraestructura | Sonnet | `spec/0a-1-plataforma` | ✅ integrada | 2 | #17 | — |
| **CK1** Checkpoint de base (revisión exhaustiva del lead) | lead | `fix/ck1-base` | 👀 en revisión del autor | — | — | [Informe](checkpoints/CK1.md): 1 bloqueante (H1) y 4 menores; corrección en curso |
| 0a.2 auth | — | `spec/0a-2-auth` | ⏳ | — | — | Tras CK1 |
| 0a.2 catalog | — | `spec/0a-2-catalog` | ⏳ | — | — | Tras CK1 |
| 0a.2 settings | — | `spec/0a-2-settings` | ⏳ | — | — | Tras CK1 |
| 0a.2 order | — | `spec/0a-2-order` | ⏳ | — | — | Tras CK1 |
| 0a.2 payment | — | `spec/0a-2-payment` | ⏳ | — | — | Tras CK1 |
| 0a.2 delivery | — | `spec/0a-2-delivery` | ⏳ | — | — | Tras CK1 |
| 0a.2 notification | — | `spec/0a-2-notification` | ⏳ | — | — | Tras CK1 |
| 0a.3 CI, humo y Postman | — | `spec/0a-3-ci` | ⏳ | — | — | Tras los 7 esqueletos |

## Próximas olas

| Ola | Specs | Estado |
|---|---|---|
| 1 | 0b | ⏳ |
| — | **CK2** Checkpoint de base (0a + 0b) | ⏳ |
| 2a | 1, 2, 3 | ⏳ |
| 2b | 4, 5, 6, 7 | ⏳ |
| 3 | 8 | ⏳ |

## Registro

| Fecha | Evento |
|---|---|
| 2026-10-19 | Estrategia aprobada (v3). Brief de 0a.1 listo. |
| 2026-10-19 | 0a.1 integrada (#17, 2 rondas). CK1: informe con H1–H5; brief de 0a.2 completo. |
| 2026-10-19 | 0a.1 lanzado; fase de lectura revisada (15 dudas resueltas; CA-0a-07 y 18 vuelven a 0a.3 según la spec). |
