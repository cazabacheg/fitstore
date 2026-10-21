# Brief — SPEC-6 Envíos

> Preparado por el lead · 2026-10-20 · Spec: [SPEC-6](../../specs/SPEC-6-envios.md) **v6** · Modelo del implementador: Sonnet · Ola 2b
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Recorte: [extensión de alcance](../extension-alcance.md)

Un agente, **backend y luego frontend**. Rama `spec/6-envios`, alcance [`SPEC-6.alcance`](SPEC-6.alcance). **Plazo real:** sustentación el sábado 24.

## Validación del brief (estrategia B2)

Copiada de la sección 7 de la SPEC-6 v6. **Fuera de alcance (extensión):** CA-6-28 a 30 (HU-6-08 y P-6-03, entregas del día y hoja de ruta) y CA-6-33 y 34 (HU-6-10, devolución a la tienda).

| CA | Sección 7: tipo · dónde | Dueño | Nota del lead |
|---|---|---|---|
| 01–06 | Datos + API · `TarifaEnvioRepositoryTest`, `CotizacionEnvioServiceTest`, `EnvioControllerTest` | SPEC-6 | Lo consume el checkout de la SPEC-4 (en paralelo) |
| 07–12 | API + componente · `TarifaServiceTest`, `AdminTarifaControllerTest`, `tarifas.page.spec.ts` | SPEC-6 | Selector de ubigeo de `ui/` con `hasta` |
| 13–15 | Datos + API · `AgenciaRepositoryTest`, `AgenciaServiceTest` | SPEC-6 | — |
| 16–18 | Datos + API · `RepartidorRepositoryTest`, `RepartidorServiceTest` | SPEC-6 | — |
| 19 | Integración · `ConsumidorPedidosIT` | SPEC-6 | — |
| 20–27 | Unitaria + API · `MaquinaEstadosEntregaTest`, `EntregaServiceTest`, contenido del evento | SPEC-6 | Sin la transición a DEVUELTA (extensión) |
| 31–32 | Datos + API · `EntregaSpecificationsTest` | SPEC-6 | — |
| Todas | Postman · `postman/delivery.postman_collection.json` | SPEC-6 | Sin la devolución a tienda |

`scripts/ca-cobertura.sh 6` debe dar **29 de 29**.

## 1. Lectura obligatoria

1. `CLAUDE.md`, este brief y `extension-alcance.md`.
2. SPEC-6 v6 completa (salta lo marcado como extensión).
3. Micro [`delivery-service.md`](../../analisis/micro/delivery-service.md).
4. Contratos: `delivery-service.yaml`, `comun.yaml`, `EntregaActualizada` y `PedidoListoDespacho` en `fitstore-eventos.yaml`.
5. El código de 0a de delivery (entidades, `V1`, `V2` con agencias y tarifas semilla) y la SPEC-0b (R2, R11), `shared/public-api.ts` (`deliveryApi`, `UbigeoCatalogo`).

En tu primer mensaje (FASE 1, sin código): qué leíste, plan en 5–10 líneas y dudas.

## 2. Qué construyes

- **Backend:** tarifas por ubigeo con búsqueda jerárquica, opciones y cotización de envío, agencias, repartidores, entregas desde `PedidoListoDespacho` (idempotente), máquinas de estado local y agencia, `EntregaActualizada` (sin el teléfono del repartidor), búsqueda con filtros y contadores, datos demo, `ContratoOpenApiTest`.
- **Backoffice:** `ENTREGAS_ROUTES` (`''` P-6-01 y `:id` P-6-02; `dia` se queda con su "Próximamente"), `REPARTIDORES_ROUTES` (P-6-04), `TARIFAS_ROUTES` (P-6-05) y `AGENCIAS_ROUTES` (P-6-06).
- Usa `OpenApiConfig` como catalog y settings para que pase `scripts/contrato.sh delivery-service`.

**Núcleo (cobertura completa):** búsqueda jerárquica de tarifas, `CotizacionEnvioService`, `MaquinaEstadosEntrega`, consumidor de `PedidoListoDespacho`, `EntregaService`.
