# FitStore — Tablero de implementación

> Lo actualiza el lead en cada cambio de estado ([estrategia multiagente](estrategia-multiagente.md), sección 8).
> Estados: ⏳ en espera · 📝 brief listo · 🔨 en curso · 🔍 en verificación (ronda N) · 👀 en revisión del autor · ✅ integrada · ⛔ bloqueada

## Ola 0 — SPEC-0a ✅ cerrada

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
| 0a.3 CI, humo y Postman | Sonnet | `spec/0a-3-ci` | ✅ integrada | 2 | #29 | Primer CI real en verde |

## Ola 1 — SPEC-0b ✅ cerrada

| Paso | Agente | Rama | Estado | Ronda | PR | Bloqueos / notas |
|---|---|---|---|---|---|---|
| 0b.1 Workspace, `shared` y CI | Sonnet | `spec/0b-1-base` | ✅ integrada | 1 | #33 | — |
| 0b.2 Storefront | Sonnet | `spec/0b-2-storefront` | ✅ integrada | 1 | #35 | En paralelo con 0b.3 |
| 0b.3 Backoffice | Sonnet | `spec/0b-3-backoffice` | ✅ integrada | 1 | #36 | En paralelo con 0b.2 |
| **CK2** Checkpoint de base (0a + 0b) | lead | `fix/ck2-base`, `docs/ck2`, `fix/rol-menu` | ✅ aprobado | — | #37, #38, #39 | [Informe](checkpoints/CK2.md): H1–H7 corregidos |

## Ola en curso: 2a — SPEC-1, 2 y 3

| Paso | Agente | Rama | Estado | Ronda | PR | Bloqueos / notas |
|---|---|---|---|---|---|---|
| SPEC-1 Identidad | Sonnet | `spec/1-identidad` | 📝 brief listo | — | — | [Brief](briefs/SPEC-1.md); retira el JWKS de prueba (A5) |
| SPEC-2.1 Catálogo: backend | Sonnet | `spec/2-catalogo` | 📝 brief listo | — | — | [Brief](briefs/SPEC-2.md) |
| SPEC-2.2 Catálogo: frontend | Sonnet | `spec/2-catalogo` | ⏳ | — | — | Tras 2.1, en la misma rama |
| SPEC-3 Tienda y reclamos | Sonnet | `spec/3-tienda` | 📝 brief listo | — | — | [Brief](briefs/SPEC-3.md) |

## Ola 2b — SPEC-4, 5 y 6 (recortadas)

| Paso | Agente | Rama | Estado | Ronda | PR | Bloqueos / notas |
|---|---|---|---|---|---|---|
| SPEC-4 Pedidos | Sonnet | `spec/4-pedidos` | 📝 brief listo | — | — | [Brief](briefs/SPEC-4.md) |
| SPEC-5 Pagos | Sonnet | `spec/5-pagos` | 📝 brief listo | — | — | [Brief](briefs/SPEC-5.md); Mercado Pago + simulado |
| SPEC-6 Envíos | Sonnet | `spec/6-envios` | 📝 brief listo | — | — | [Brief](briefs/SPEC-6.md); entra al terminar 2.2 |

## Próximas olas

| Ola | Specs | Estado |
|---|---|---|
| 2b | 4, 5, 6, 7 | ⏳ |
| 3 | 8 | ⏳ |

## Registro

| Fecha | Evento |
|---|---|
| 2026-10-20 | **Recorte de alcance** (decisión del autor): P-2-09 y 11 historias de las specs 4–7 pasan a [extensión de alcance](extension-alcance.md); specs 4–7 de 164 a 125 criterios. Pasarela real: Mercado Pago. |
| 2026-10-20 | CK2 corregido e integrado (#37, #38; rol en el menú, #39). Briefs de la ola 2a: SPEC-1, SPEC-2 en dos pasos y SPEC-3; stack v10 (WebP, Markdown, script de fotos). |
| 2026-10-20 | SPEC-0b cerrada (#33, #35, #36). CK2: base desde cero en verde; 2 bloqueantes (rutas del backoffice por pantalla, `.env` del *smoke*) y 5 correcciones más, aprobadas por el autor; se corrigen en `fix/ck2-base`. |
| 2026-10-20 | SPEC-0a cerrada (#29, primer CI real en verde). `.env.example` completado (#28). Brief de 0b listo. |
| 2026-10-20 | Los 7 esqueletos de 0a.2 integrados (#20–#26; notification en 2 rondas, el resto en 1). Brief de 0a.3 listo. |
| 2026-10-19 | Estrategia aprobada (v3). Brief de 0a.1 listo. |
| 2026-10-19 | 0a.1 integrada (#17, 2 rondas). CK1: informe con H1–H5; brief de 0a.2 completo. |
| 2026-10-19 | 0a.1 lanzado; fase de lectura revisada (15 dudas resueltas; CA-0a-07 y 18 vuelven a 0a.3 según la spec). |
