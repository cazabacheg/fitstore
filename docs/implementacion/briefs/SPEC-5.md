# Brief — SPEC-5 Pagos

> Preparado por el lead · 2026-10-20 · Spec: [SPEC-5](../../specs/SPEC-5-pagos.md) **v10** · Modelo del implementador: Sonnet · Ola 2b
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Recorte: [extensión de alcance](../extension-alcance.md)

Un agente, **backend y luego frontend**. Rama `spec/5-pagos`, alcance [`SPEC-5.alcance`](SPEC-5.alcance). **Plazo real:** sustentación el sábado 24.

## Validación del brief (estrategia B2)

Copiada de la sección 7 de la SPEC-5 v10. **Fuera de alcance (extensión):** CA-5-09 a 12 (Culqi tarjeta), CA-5-13 y 14 (Yape), CA-5-30 a 32 y 39 a 40 (reembolsos automáticos), CA-5-36 a 38 y P-5-05 (reembolsos fallidos). **Pasarelas en esta entrega:** Mercado Pago (sandbox) y el proveedor simulado.

| CA | Sección 7: tipo · dónde | Dueño | Nota del lead |
|---|---|---|---|
| 01–02 | Integración · `IntencionPagoConsumidorIT` | SPEC-5 | — |
| 03–08 | API + componente · `PagoControllerTest`, `pago.page.spec.ts` | SPEC-5 | Medios disponibles: Mercado Pago y simulado (Yape y Culqi no se ofrecen) |
| 15 | Unitaria + integración · `MercadoPagoProviderTest` (WireMock), `CobroServiceTest`, `LogSinDatosSensiblesTest` | SPEC-5 | Solo la parte de Mercado Pago de esa fila |
| 16–19 | Integración · `DobleCobroIT`, `IdempotenciaCobroTest`, timeout WireMock; 429 en Postman | SPEC-5 | — |
| 20–23 | API + integración · `WebhookControllerTest`, `VerificadorPagosTest`, circuito abierto | SPEC-5 | Webhook de Mercado Pago con su firma |
| 24–25 | Unitaria + API · `SimuladoProviderTest`, configuración habilitada/deshabilitada | SPEC-5 | Es el que se usa en la demo si no hay credenciales |
| 26–29 | Datos + integración · `BoletaServiceTest` (PDFBox), `CorrelativoBoletaIT` (20 concurrentes), Garage caído, descarga ajena 404 | SPEC-5 | Garage en Testcontainers como catalog (`dxflrs/garage:v2.4.1`) |
| 33–35 | API + componente · `AdminPagoControllerTest` (filtros, 403 a OPERADOR) | SPEC-5 | Pantallas P-5-03 y P-5-04 (sin P-5-05) |
| Manual | Sandbox real · `docs/pruebas/poc-pagos.md` | Lead / autor | Solo si llegan credenciales de prueba de Mercado Pago; el agente deja el guion |
| Todas | Postman · `postman/payment.postman_collection.json` | SPEC-5 | Sin los pasos de reembolso (extensión) |

`scripts/ca-cobertura.sh 5` debe dar **26 de 26**.

## 1. Lectura obligatoria

1. `CLAUDE.md`, este brief y `extension-alcance.md`.
2. SPEC-5 v10 completa (salta lo marcado como extensión).
3. Micro [`payment-service.md`](../../analisis/micro/payment-service.md): intención, cobro con bloqueo, `PaymentProvider` y registro de proveedores, webhooks, verificador, boleta.
4. Contratos: `payment-service.yaml`, `comun.yaml`, canales de payment en `fitstore-eventos.yaml` y los eventos de order que consume.
5. El código de 0a de payment (entidades, `V1`, `PostgresPrueba`) y `shared/public-api.ts` (`paymentApi`).
6. Diseños `Pago`, `PagoKallpaMovil`, `PagoResultado` y su versión móvil.

En tu primer mensaje (FASE 1, sin código): qué leíste, plan en 5–10 líneas y dudas.

## 2. Qué construyes

- **Backend:** intención por pedido desde `PedidoCreado` (idempotente), cobro con bloqueo e idempotencia, `MercadoPagoProvider` (Payment Brick en el navegador, cargo en el backend) y `SimuladoProvider`, webhooks verificados, verificador de pagos en curso con circuit breaker, boleta en PDF con correlativo en bucket privado y `/internal/v1/boletas/{numero}`, eventos `PagoConfirmado`, `PagoRechazado` y `BoletaEmitida`, backoffice de pagos, `ContratoOpenApiTest`.
- **Tienda:** `PAGO_ROUTES` (P-5-01 y P-5-02, misma ruta `/pago/:codigo`).
- **Backoffice:** `PAGOS_ROUTES` (`''` P-5-03 y `:id` P-5-04). `REEMBOLSOS_ROUTES` se queda con su "Próximamente".
- **Credenciales:** ninguna real en el repo. Variables de Mercado Pago vacías en `infra/.env.example` (ya existen `MP_PUBLIC_KEY` y `MP_WEBHOOK_SECRET`; agrega solo lo que falte, en la sección de pagos).

**Núcleo (cobertura completa):** `CobroService` (bloqueo, idempotencia, doble cobro), `MercadoPagoProvider`, `SimuladoProvider`, webhooks (firma, duplicado), verificador, `BoletaService` y su correlativo.
