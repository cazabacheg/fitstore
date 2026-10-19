# FitStore — Specs

> Corte **A** (decidido 2026-10-18): **una spec por servicio, con sus pantallas**. Cada servicio backend tiene una sola spec dueña → dos specs en paralelo nunca tocan el mismo servicio. Las dependencias entre specs se resuelven con contratos (OpenAPI / AsyncAPI) y WireMock.

| Spec | Dueña de (backend) | Pantallas | Depende de | Paralelo |
|---|---|---|---|---|
| [0a](SPEC-0a-plataforma-backend.md) Plataforma backend + esqueletos ✅ | config, Eureka, gateway, `fitstore-common`, infra, **esqueleto de los 7 servicios** | — | — | Sola (los 7 esqueletos entre sí, en paralelo) |
| [0b](SPEC-0b-fundaciones-frontend.md) Fundaciones del frontend ✅ | — | workspace Angular, `shared`, layouts, rutas registradas, tema white-label | 0a, contratos, sistema de diseño | Sola |
| [1](SPEC-1-identidad.md) Identidad ✅ | auth | login, registro, perfil, direcciones, staff | 0a, 0b | Sí |
| [2](SPEC-2-catalogo-inventario.md) Catálogo e inventario ✅ | catalog | backoffice: catálogo e inventario · tienda: listado, filtros, detalle | 0a, 0b | Sí |
| [3](SPEC-3-tienda-reclamos.md) Tienda y reclamos ✅ | settings | configuración de tienda, Libro de Reclamaciones | 0a, 0b | Sí |
| [4](SPEC-4-pedidos.md) Pedidos ✅ | order | carrito, checkout, mis pedidos, pedidos (backoffice) | 0a, 0b | Sí (contra mocks de 1, 2, 3, 6) |
| [5](SPEC-5-pagos.md) Pagos ✅ | payment | página de pago, pagos y reembolsos | 0a, 0b | Sí |
| [6](SPEC-6-envios.md) Envíos ✅ | delivery | entregas, repartidores, tarifas, agencias | 0a, 0b | Sí |
| [7](SPEC-7-notificaciones.md) Notificaciones ✅ | notification | registro de envíos, editor de plantillas | 0a, 0b | Sí |
| [8](SPEC-8-integracion-nube-demo.md) Integración, nube y demo ✅ | pruebas E2E, dashboard, compose cloud, Caddy, DuckDNS, SES, `deploy.yml`, datos de demo en la nube; Garage en la VM (Azure Blob diferido) | dashboard | 1–7 | Al final |

## Reglas de convivencia

1. **Un dueño por servicio y por carpeta.** Cada spec lista en su sección 8 lo que crea o modifica; nadie toca lo ajeno.
2. **Congelados tras su spec:** `backend/fitstore-common`, `backend/api-gateway`, `infra/config-repo/application.yml`, `frontend/projects/shared`, rutas y menús generales. Un cambio se pide y se hace en un PR aparte, de uno en uno.
3. **Migraciones:** `V1` (esquema completo) la crea 0a; las siguientes usan `VAAAAMMDD_HHMM__…` para no chocar.
4. **Permisos con `@PreAuthorize`** por método; sin configuración central de rutas por servicio.
5. **Un consumidor por tipo de evento**, en su propio archivo.
6. **Clientes de API del frontend generados** desde OpenAPI en `shared/api`; nunca se editan a mano.
7. **Una rama / worktree por spec desde `dev`**, PR a `dev` con *squash*, de uno en uno con CI en verde; promociones `dev → stage → main` por PR (ADR-021). Nunca push directo a `main` ni a `stage`.
8. Commits sin atribución de IA; autor `cazabacheg`.

## Proceso y diseño

Specs (con bocetos de baja fidelidad) → contratos (OpenAPI / AsyncAPI) → **diseño**: sistema de diseño + ~12 pantallas clave de alta fidelidad en **dos marcas** (demuestra el white-label) → se enlazan en cada pantalla de las specs (campo "Diseño de alta fidelidad") → estrategia multiagente → implementación. La 0b no se implementa sin el sistema de diseño.

Pantallas clave (12 grupos, 14 pantallas): tienda (landing, listado, detalle de producto, carrito, checkout, pago y su resultado, mis pedidos y su detalle; escritorio y móvil) y backoffice (dashboard, lista de pedidos, detalle de pedido, formulario de producto, configuración de tienda). El resto deriva del sistema de diseño y su boceto.

Plantilla: [`_plantilla.md`](_plantilla.md) (v3: criterios dentro de cada historia; historias enlazadas a sus pantallas `P-XX-NN`; bocetos en `img/`; cada pantalla reserva su diseño de alta fidelidad).
