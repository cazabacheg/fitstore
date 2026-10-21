# Brief: SPEC-8 Integración, nube y demo

> Preparado por el lead · 2026-10-21 · Spec: [SPEC-8](../../specs/SPEC-8-integracion-nube-demo.md) **v3** · Modelo del implementador: Sonnet · Ola 4
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Recorte y desvíos: [extensión de alcance](../extension-alcance.md)

Un agente. Rama `spec/8-nube-demo`, alcance [`SPEC-8.alcance`](SPEC-8.alcance). **Plazo real:** sustentación el sábado 24; la tienda debe estar publicada el viernes 23.

## Lo que ya validó el lead (no se repite)

- La plataforma de `dev` corre en la VM de Azure (B2as_v2, Chile Central, IP estática `57.156.71.26`): humo completo en verde y compra de punta a punta con correos en Mailpit.
- Memoria acotada de la JVM y 512 MiB por servicio (PR #60). El arranque completo tarda unos 130 s con 2 vCPU.
- Auth ya cambia las contraseñas demo por `DEMO_PASSWORD` en el perfil demo (`DatosDemoNube`, CA-1-39).

## Desvíos acordados (ver `extension-alcance.md`)

- **Correo:** Mailpit en la VM, sin publicar, visto por túnel SSH. Sin SES (CA-8-24 fuera).
- **DNS:** IP estática; el autor registra la IP en DuckDNS una vez. Sin contenedor de DuckDNS (CA-8-19 fuera).
- **Imágenes:** se construyen **en la VM** (`deploy.yml` copia el código por SSH y corre `docker compose build`), no en GHCR: el repo es privado y el espacio gratuito de paquetes privados no alcanza para 9 imágenes. La etiqueta del commit se guarda en el `.env` de la VM para saber qué está desplegado.
- **Dashboard:** sin ventas ni top de productos (HU-4-11 es extensión). Las entregas salen de los contadores de `GET /admin/entregas` (el resumen del día es extensión) y su tarjeta lleva a `/entregas`.
- **E2E de postventa** (CA-8-08) fuera: depende de los reembolsos.

## Validación del brief (estrategia B2)

| CA | Dónde | Nota del lead |
|---|---|---|
| 01–05 | `dashboard.page.spec.ts` | CA-01 y 03 con el recorte de arriba. OPERADOR ve lo mismo (ya no hay ventas que ocultar) |
| 06, 07, 09, 10, 11 | `frontend/e2e/*.spec.ts` (Playwright) en `stage.yml` | CA-06 sin fusión de carrito: el visitante se registra y luego arma el carrito. Pago con el simulado y correos por la API de Mailpit (I3) |
| 12 | `frontend/lighthouserc.json` en `stage.yml` | Umbral 85; si no llega, se reporta el puntaje y no bloquea |
| 13 | k6 | Lo mide el lead en Lyanna |
| 14–18 | `infra/docker-compose.cloud.yml`, `infra/caddy/`, verificación en la VM | HTTPS con Caddy en `tienda.<sub>` y `admin.<sub>` (el autor da el subdominio) |
| 20–22 | `.github/workflows/deploy.yml` | Con el desvío de imágenes. Secretos en GitHub Secrets |
| 23, 25 | `docs/demo/guion.md` | 23 solo si llegan credenciales de Mercado Pago |
| 26–28 | `infra/scripts/respaldo.sh`, `docs/despliegue.md` | La alerta de presupuesto (US$25) ya existe |
| 29–31 | `docs/demo/guion.md`, verificación en la VM | — |

`scripts/ca-cobertura.sh 8` debe dar el total menos los excluidos (19, 24 y 08).

## Reglas propias

- **Nunca dejes la VM encendida al terminar una sesión de trabajo:** `az vm deallocate -g fitstore-rg -n fitstore-vm`. Enciéndela solo para verificar.
- Ningún secreto en el repo: `.env` de la nube y claves en GitHub Secrets y en la VM. gitleaks debe seguir en verde.
- Congelados salvo lo que lista el `.alcance`: servicios de negocio, `fitstore-common`, gateway y `shared` son de solo lectura.
