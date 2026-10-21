# Brief — SPEC-4 Pedidos

> Preparado por el lead · 2026-10-20 · Spec: [SPEC-4](../../specs/SPEC-4-pedidos.md) **v11** · Modelo del implementador: Sonnet · Ola 2b
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Estrategia: [estrategia multiagente](../estrategia-multiagente.md) · Recorte: [extensión de alcance](../extension-alcance.md)

Un agente, **backend y luego frontend**. Rama `spec/4-pedidos`, alcance [`SPEC-4.alcance`](SPEC-4.alcance).

**Plazo real:** la sustentación es el sábado 24. Prioriza el camino de punta a punta (carrito → checkout → pedido → estados) antes que los casos de borde, pero sin saltarte los criterios: todos deben tener su prueba.

## Validación del brief (estrategia B2)

Copiada de la sección 7 de la SPEC-4 v11. **Fuera de alcance (extensión):** CA-4-09 a 12 (HU-4-02), CA-4-51 a 53 (HU-4-11) y CA-4-54 a 59 (HU-4-12). `ca-cobertura.sh` ya los excluye.

| CA | Sección 7: tipo · dónde | Dueño | Nota del lead |
|---|---|---|---|
| 01–08 | API + componente · `CarritoServiceTest`, `CarritoRepositoryTest` (Redis), `carrito.page.spec.ts` | SPEC-4 | El carrito anónimo usa `X-Carrito-Id` (ya lo pone el interceptor de `shared`); actualiza `CarritoContador` |
| 13–18 | API + componente · `CheckoutServiceTest` (Feign en WireMock), `checkout.page.spec.ts` | SPEC-4 | El checkout reutiliza `DialogoDireccion` de la SPEC-1 (`features/cuenta/direcciones`, solo lectura) |
| 19–25 | Unitaria + datos + API · `CheckoutServiceTest`, `PedidoRepositoryTest`, `MontosPedidoTest` | SPEC-4 | — |
| 26 | Carga · `k6/checkout.js` | SPEC-4 | Deja el script listo; el lead lo mide en Lyanna con la plataforma levantada |
| 27–32 | Integración · `SagaPedidoIT` (RabbitMQ Testcontainers) | SPEC-4 | — |
| 33–37 | API + componente · `PedidoControllerTest`, `detalle-pedido.page.spec.ts` | SPEC-4 | — |
| 38–39, 49–50 | Unitaria + API · `MaquinaEstadosPedidoTest`, `CancelacionTest` | SPEC-4 | Sin reembolso automático (extensión): la cancelación de un pedido pagado publica `PedidoCancelado` con `fuePagado`, y nada más |
| 40–42 | Datos + API · `PedidoSpecificationsTest` | SPEC-4 | — |
| 43–48, 60 | API + integración · `AdminPedidoControllerTest`, `SagaPedidoIT` (`EntregaActualizada`) | SPEC-4 | — |
| Todas | Postman · `postman/order.postman_collection.json` | SPEC-4 | Sin los pasos de fusión ni de reembolso postventa (extensión) |

`scripts/ca-cobertura.sh 4` debe dar **47 de 47**.

## 1. Lectura obligatoria

1. `CLAUDE.md`, este brief y [`extension-alcance.md`](../extension-alcance.md).
2. [SPEC-4](../../specs/SPEC-4-pedidos.md) v11 completa (salta las historias marcadas como extensión).
3. Micro [`order-service.md`](../../analisis/micro/order-service.md): carrito en Redis, checkout con idempotencia, máquina de estados, saga, expiración, consultas.
4. Contratos: `order-service.yaml`, `comun.yaml`, los canales que publica y consume order en `fitstore-eventos.yaml`, y como **consumidor** (mocks con WireMock): de `catalog-service.yaml` la cotización y `/internal/v1/inventario/reservas`; de `auth-service.yaml` el cliente y sus direcciones; de `delivery-service.yaml` opciones y cotización de envío; de `settings-service.yaml` los parámetros públicos.
5. El código de 0a de order (entidades, `CarritoRepository`, `EntornoPrueba`, `V1`) y la SPEC-0b (R2, R8, R11), `shared/public-api.ts` (`orderApi`, `CarritoContador`, `SesionStore`).
6. Diseños `Carrito`, `CarritoKallpaMovil`, `Checkout`, `CheckoutKallpaMovil`, `MisPedidos`, `MiPedido` y sus versiones móviles en `docs/diseno/pantallas/`.

En tu primer mensaje (FASE 1, sin código): qué leíste, plan en 5–10 líneas y dudas.

## 2. Qué construyes

- **Backend:** carrito anónimo y de cliente en Redis con cotización, resumen y confirmación del checkout (idempotente, precio congelado, reserva de stock por contrato, `PedidoCreado`), máquina de estados con historial (fecha y responsable), expiración, consumo de `PagoConfirmado`, `PagoRechazado`, `BoletaEmitida` y `EntregaActualizada`, cancelación por cliente y por staff, consultas del cliente y del backoffice con filtros y contadores, datos demo (`R__pedidos_demo.sql` con los UUID de clientes y los SKU de `docs/demo/ids-demo.md`), `ContratoOpenApiTest`.
- **Tienda:** `CARRITO_ROUTES` (P-4-01), `CHECKOUT_ROUTES` (P-4-02), `MIS_PEDIDOS_ROUTES` (`''` P-4-03 y `:codigo` P-4-04).
- **Backoffice:** `PEDIDOS_ROUTES` (`''` P-4-05 con `FsTabla` R11 y `:id` P-4-06).
- **Postman y k6** según la sección 7.

## 3. Reglas propias

- Las llamadas a catalog, auth, delivery y settings se prueban con WireMock contra sus contratos: catalog ya está implementado (SPEC-2), delivery se implementa en paralelo.
- Usa `OpenApiConfig` como catalog y settings para que `scripts/contrato.sh order-service` pase (springdoc genera diferencias de forma con el contrato).
- Imágenes de prueba exactas: `postgres:17.10`, `redis:8.10.1`, `rabbitmq:4.3.5-management`.
- Congelados: `shared`, `ui/`, `layout/`, `app.routes.ts`, gateway, `fitstore-common`. No toques las pruebas de 0b.

**Núcleo (cobertura completa):** `CheckoutService` (idempotencia, montos, reserva, dependencia caída), `MaquinaEstadosPedido`, consumidores de la saga, expiración, cancelación, filtros del backoffice.
