# Brief: SPEC-7 Notificaciones

> Preparado por el lead · 2026-10-21 · Spec: [SPEC-7](../../specs/SPEC-7-notificaciones.md) **v6** · Modelo del implementador: Sonnet · Ola 3
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Recorte: [extensión de alcance](../extension-alcance.md)

Un agente, **backend y luego frontend**. Rama `spec/7-notificaciones`, alcance [`SPEC-7.alcance`](SPEC-7.alcance). **Plazo real:** sustentación el sábado 24. Prioriza el camino de la demo: evento → envío → Mailpit con la marca y la boleta adjunta.

## Validación del brief (estrategia B2)

Copiada de la sección 7 de la SPEC-7 v6. **Fuera de alcance (extensión):** CA-7-23 a 27 y CA-7-30 (HU-7-06, P-7-02 y P-7-03, edición de textos y "Enviar prueba") y CA-7-28 (HU-7-07, limpieza de retención). `ca-cobertura.sh` ya los excluye.

| CA | Sección 7: tipo · dónde | Dueño | Nota del lead |
|---|---|---|---|
| 01–07 | Integración · `EnrutadorEventosIT` (RabbitMQ Testcontainers) | SPEC-7 | Un manejador por evento (N2). CA-04 se prueba desactivando la plantilla en la base (no hay pantalla). CA-06 (`PagoReembolsado`) y CA-07 (`DEVUELTA`) se implementan aunque hoy nadie publique esos eventos: el manejador escucha el contrato |
| 08–11 | Unitaria + integración · `DespachadorCorreosTest` (reloj simulado, reintentos 1-5-15-60, `SKIP LOCKED`), SMTP caído con GreenMail | SPEC-7 | Ritmo por entorno (N6) |
| 12–14 | Integración · `ConfirmacionConBoletaIT` (GreenMail, payment en WireMock) | SPEC-7 | Boleta de `/internal/v1/boletas/{numero}` (SPEC-5, ya integrada) |
| 15–18 | Unitaria · `RenderizadorCorreoTest`, `MarcaTiendaTest` | SPEC-7 | Las 18 plantillas con sus textos predeterminados (N3) |
| 19–22 | Datos + API + componente · `EnvioCorreoRepositoryTest`, `AdminNotificacionControllerTest`, `registro-envios.page.spec.ts` | SPEC-7 | Panel lateral de detalle (N1) |
| 29 | Manual · guion `docs/pruebas/demo-correos.md` y carpeta `CA-7-29` en Postman | Lead / autor | El agente deja el guion; el lead lo corre con la plataforma levantada |
| Todas | Postman · `postman/notification.postman_collection.json` | SPEC-7 | Registro, filtros y reenvío (sin editar, vista previa, restaurar ni prueba) |

`scripts/ca-cobertura.sh 7` debe dar **23 de 23**.

## 1. Lectura obligatoria

1. `CLAUDE.md`, este brief y `extension-alcance.md`.
2. SPEC-7 v6 completa (salta lo marcado como extensión).
3. Micro [`notification-service.md`](../../analisis/micro/notification-service.md): catálogo 4.1, flujo 4.2, boleta 4.3, marca 4.4, endpoints 4.5.
4. Contratos: `notification-service.yaml`, `comun.yaml` y los canales que consume en `fitstore-eventos.yaml` (auth, order, payment, delivery, catalog y settings).
5. El código de 0a de notification (entidades, `V1` a `V3`, `messaging/`) y la SPEC-0b (R2, R11), `shared/public-api.ts` (`notificationApi`).
6. Cómo publican sus eventos SPEC-1, 3, 4, 5 y 6 (ya en `dev`), para que los nombres de campos coincidan con lo que realmente sale.

En tu primer mensaje (FASE 1, sin código): qué leíste, plan en 5–10 líneas y dudas.

## 2. Qué construyes

- **Backend:** enrutador con un manejador por evento hacia los 14 correos al cliente y las 4 alertas a la tienda, copia local de la marca y de los pedidos, render Thymeleaf (HTML y texto plano, `layout.html` de P-7-04), despachador con ritmo y reintentos, boleta adjunta con respaldo de 10 min, registro con filtros y reenvío, datos demo del registro (perfil `dev` y `demo`) alineados con los pedidos FS-000101 a 111 de `docs/demo/ids-demo.md`, `ContratoOpenApiTest`.
- **Endpoints de extensión** (`/admin/notificaciones/plantillas/**`: listar, editar, restaurar, vista previa y prueba): stubs con la firma del contrato que responden 501 `EXTENSION_DE_ALCANCE`, como en SPEC-4, 5 y 6. Sus pruebas no llevan id de CA.
- **Backoffice:** `NOTIFICACIONES_ROUTES` `''` (P-7-01) con `data: { pantalla: 'notificaciones' }`. La ruta `plantillas` se queda con su "Próximamente".
- Usa `OpenApiConfig` como catalog y settings para que pase `scripts/contrato.sh notification-service`.

**Núcleo (cobertura completa):** `EnrutadorEventos` y sus manejadores, `DespachadorCorreos`, boleta con respaldo, `RenderizadorCorreo`, copia de la marca.

## 3. Reglas propias

- Mailpit en dev (ya está en el compose), GreenMail en las pruebas. SES queda para la SPEC-8: deja lista la configuración `cloud` sin credenciales.
- Imágenes de prueba exactas: `postgres:17.10`, `rabbitmq:4.3.5-management`.
- Servicios de API del frontend con import profundo (`shared/api/notification-service`), como dice `CLAUDE.md`.
- Congelados: `shared`, `ui/`, `layout/`, `app.routes.ts`, gateway, `fitstore-common`. No toques las pruebas de 0b.
