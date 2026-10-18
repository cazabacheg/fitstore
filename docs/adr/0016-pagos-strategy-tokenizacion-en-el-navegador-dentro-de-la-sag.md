# 16. Pagos: Strategy + tokenización en el navegador, dentro de la saga

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Decisión:** interfaz `PaymentProvider` (`crearCargo`, `reembolsar`, `verificarWebhook`, `consultarEstado`) con `CulqiProvider`, `MercadoPagoProvider` y un `SimuladoProvider` de respaldo para la demo. El proveedor se elige según lo habilitado en `settings-service` y lo que escoja el cliente.
- La tarjeta se tokeniza en el frontend (Culqi Checkout / Mercado Pago Bricks); el backend solo recibe tokens. Webhooks validados (firma) e idempotentes.
- **Relación con la saga (ADR-009):** cada estrategia implementa tanto la acción (cargo) como su compensación (reembolso), de modo que la saga trata a todos los proveedores igual. El cargo es el **paso pivote**: antes de él, compensar es anular la intención; después, es reembolsar. El resultado del proveedor (respuesta síncrona o webhook, lo que llegue primero) se normaliza a `PagoConfirmado` / `PagoRechazado`.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
