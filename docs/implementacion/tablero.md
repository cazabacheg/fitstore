# FitStore — Tablero de implementación

> Lo actualiza el lead en cada cambio de estado ([estrategia multiagente](estrategia-multiagente.md), sección 8).
> Estados: ⏳ en espera · 📝 brief listo · 🔨 en curso · 🔍 en verificación (ronda N) · 👀 en revisión del autor · ✅ integrada · ⛔ bloqueada

## Ola en curso: 0 — SPEC-0a

| Paso | Agente | Rama | Estado | Ronda | PR | Bloqueos / notas |
|---|---|---|---|---|---|---|
| 0a.1 Monorepo, común e infraestructura | Sonnet | `spec/0a-1-plataforma` | ✅ integrada | 2 | #17 | — |
| **CK1** Checkpoint de base (revisión exhaustiva del lead) | lead | `fix/ck1-base` | ✅ aprobado | — | #18, #19 | [Informe](checkpoints/CK1.md) |
| 0a.2 auth | Sonnet | `spec/0a-2-auth` | ✅ integrada | 1 | #20 | — |
| 0a.2 catalog | Sonnet | `spec/0a-2-catalog` | ✅ integrada | 1 | #22 | — |
| 0a.2 settings | Sonnet | `spec/0a-2-settings` | ✅ integrada | 1 | #21 | — |
| 0a.2 order | Sonnet | `spec/0a-2-order` | ✅ integrada | 1 | #23 | — |
| 0a.2 payment | Sonnet | `spec/0a-2-payment` | ✅ integrada | 1 | #24 | — |
| 0a.2 delivery | Sonnet | `spec/0a-2-delivery` | ✅ integrada | 1 | #25 | — |
| 0a.2 notification | Sonnet | `spec/0a-2-notification` | ✅ integrada | 2 | #26 | — |
| 0a.3 CI, humo y Postman | — | `spec/0a-3-ci` | 📝 brief listo | — | — | Esperando la aprobación del autor para lanzar |

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
| 2026-10-20 | Los 7 esqueletos de 0a.2 integrados (#20–#26; notification en 2 rondas, el resto en 1). Brief de 0a.3 listo. |
| 2026-10-19 | Estrategia aprobada (v3). Brief de 0a.1 listo. |
| 2026-10-19 | 0a.1 integrada (#17, 2 rondas). CK1: informe con H1–H5; brief de 0a.2 completo. |
| 2026-10-19 | 0a.1 lanzado; fase de lectura revisada (15 dudas resueltas; CA-0a-07 y 18 vuelven a 0a.3 según la spec). |
