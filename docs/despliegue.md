# Despliegue y operación de FitStore en la nube

> SPEC-8 · Guía para el autor. Cubre CA-8-14 a CA-8-22 y CA-8-26 a CA-8-28.
> La plataforma corre en **una VM de Azure** (`fitstore-vm`, grupo `fitstore-rg`, B2as_v2, 2 vCPU y 8 GB, Ubuntu 24.04, Chile Central) con `docker compose`. Solo Caddy publica puertos (80 y 443).

## 1. Qué hay y dónde

| Pieza | Detalle |
|---|---|
| VM | `fitstore-vm`, IP pública **estática** `57.156.71.26`, usuario SSH `fitstore` (solo llave) |
| Código | `~/fitstore` (copia del repositorio; `desplegar.sh` la reemplaza en cada despliegue) |
| Compose | `infra/docker-compose.yml` + `infra/docker-compose.cloud.yml` |
| Variables | `~/fitstore/infra/.env` (**solo vive en la VM**, `chmod 600`; ver sección 3) |
| Claves JWT | `JWT_DIR` (fuera del repositorio; las crea `desplegar.sh` la primera vez) |
| Respaldos | `/srv/fitstore/respaldos/AAAA-MM-DD.sql.gz` (últimos 7) |
| Dominios | `tienda.<DOMINIO>` y `admin.<DOMINIO>` (DuckDNS apuntando a la IP estática) |

Desvíos acordados con respecto a la spec: correo con **Mailpit** (sin Amazon SES), IP estática sin contenedor de DuckDNS, e imágenes construidas **en la VM** (sin GHCR). Ver `docs/implementacion/extension-alcance.md`.

## 2. Primer despliegue (una sola vez)

1. **DuckDNS:** en duckdns.org crea el subdominio (por ejemplo `fitstore`) y pon la IP `57.156.71.26`. Como la IP es estática no hay que volver a tocarlo. `DOMINIO` será `fitstore.duckdns.org`; el sistema usa `tienda.fitstore.duckdns.org` y `admin.fitstore.duckdns.org` (los subdominios `tienda.` y `admin.` de DuckDNS apuntan solos a la misma IP).
2. **Azure:** la VM permite entrada en 80 y 443 (todo el mundo) y en 22 (regla `ssh-solo-llave` del NSG, abierta a todos porque los *runners* de GitHub entran por ahí para desplegar). Nada más.

   **Endurecimiento del SSH** (hecho una vez): sin contraseña ni acceso de `root` (`PasswordAuthentication no`, `PermitRootLogin no`), solo el usuario `fitstore` (`AllowUsers fitstore`), **fail2ban** que bloquea las IP con intentos fallidos y actualizaciones de seguridad automáticas. Las llaves autorizadas son la del autor y una llave ed25519 **solo para el despliegue** (la privada vive únicamente en el secreto `VM_SSH_KEY` de GitHub). Para quitarle acceso a GitHub basta borrar esa línea de `~/.ssh/authorized_keys` de la VM.
3. **`.env` de la VM:** crea `~/fitstore/infra/.env` a partir de `infra/.env.example` con las variables de la sección 3.
4. **Despliegue:** desde una copia del repositorio en tu máquina:
   ```bash
   VM_HOST=57.156.71.26 infra/scripts/desplegar.sh          # o desde GitHub: Actions > deploy > Run workflow
   ```
   El primer arranque construye las imágenes en la VM (unos 15 a 25 minutos) y los certificados de Let's Encrypt se piden solos en el primer acceso (el DNS ya debe apuntar a la VM).
5. **Respaldo diario:** en la VM, `crontab -e` y agrega
   ```
   0 3 * * * /home/fitstore/fitstore/infra/scripts/respaldo.sh >> /home/fitstore/respaldo.log 2>&1
   ```
   y crea la carpeta una vez: `sudo mkdir -p /srv/fitstore/respaldos && sudo chown fitstore: /srv/fitstore/respaldos`.

## 3. Variables que debe tener el `.env` de la VM

Partiendo de `infra/.env.example`, estas cambian o se agregan en la nube. **Nunca** se suben al repositorio.

| Variable | Valor en la nube |
|---|---|
| `DOMINIO` | `fitstore.duckdns.org` (sin `tienda.` ni `admin.`) |
| `JWT_DIR` | carpeta absoluta para las claves, p. ej. `/home/fitstore/fitstore-secretos/jwt` |
| `SWAGGER_USER` | usuario de Swagger |
| `SWAGGER_PASSWORD_HASH` | hash **bcrypt** de la contraseña de Swagger (ver abajo), **entre comillas simples** para que `$` no se interprete |
| `DEMO_PASSWORD` | contraseña de las cuentas de demostración (clientes y OPERADOR). Si queda vacía, esas cuentas se desactivan |
| `FITSTORE_ADMIN_EMAIL`, `FITSTORE_ADMIN_PASSWORD` | el ADMIN real; contraseña fuerte |
| `POSTGRES_PASSWORD` y `*_DB_PASSWORD` (7), `RABBITMQ_PASSWORD`, `CONFIG_PASSWORD`, `GARAGE_RPC_SECRET`, `GARAGE_ADMIN_TOKEN`, `S3_SECRET_KEY` | valores nuevos y largos (no los de dev) |
| `FITSTORE_PUBLIC_URL` | `https://tienda.<DOMINIO>` |
| `STOREFRONT_URL` | `https://tienda.<DOMINIO>` |
| `BACKOFFICE_URL` | `https://admin.<DOMINIO>` |
| `MEDIA_BASE_URL` | `https://tienda.<DOMINIO>/media` |
| `PAGOS_SIMULADO_HABILITADO` | `true` (el proveedor simulado queda como respaldo, CA-8-25) |
| `CULQI_*`, `MP_PUBLIC_KEY`, `MERCADOPAGO_ACCESS_TOKEN`, `MP_WEBHOOK_SECRET` | credenciales de **sandbox**, si las hay (la demo solo habilita Mercado Pago) |
| `SMTP_HOST`/`SMTP_PORT`/`SMTP_STARTTLS` | `mailpit` / `1025` / `false` (Mailpit recibe los correos) |
| `MAIL_FROM` | `"FitStore <no-responder@<DOMINIO>>"` |
| `FITSTORE_TAG` | lo escribe `desplegar.sh` (commit desplegado); no lo toques |

**Generar el hash de Swagger:**
```bash
docker run --rm caddy:2.11.3 caddy hash-password --plaintext 'LaContraseñaDeSwagger'
```
Pega el resultado como `SWAGGER_PASSWORD_HASH='$2a$14$...'` (comillas simples).

## 4. Desplegar

- **Automático:** al integrar un PR de `stage` a `main`, `deploy.yml` copia el código a la VM por SSH, construye las imágenes allí, ejecuta `docker compose up -d`, espera a que todo esté *healthy*, verifica por HTTPS y corre las colecciones Postman con el entorno `postman/entornos/cloud.json`.
- **A mano desde GitHub:** Actions > `deploy` > *Run workflow*, con la etiqueta o la rama a desplegar (CA-8-22).
- **A mano desde tu máquina:** `VM_HOST=57.156.71.26 infra/scripts/desplegar.sh [commit|rama]`.
- Secretos de GitHub que usa `deploy.yml`: `VM_HOST`, `VM_USER`, `VM_SSH_KEY` (llave solo de despliegue) y `VM_KNOWN_HOSTS`.
- La etiqueta del commit desplegado queda en `infra/.env` como `FITSTORE_TAG` y en el nombre de las imágenes (`fitstore/<servicio>:<etiqueta>`).

### Volver atrás

Ejecuta `deploy` a mano con la **etiqueta anterior** (los 12 primeros caracteres de un commit anterior, o su tag/rama). Reconstruye esa versión en la VM. Ver la etiqueta actual: `ssh fitstore@57.156.71.26 grep FITSTORE_TAG fitstore/infra/.env`. Si el despliegue falló en el *healthy*, `docker compose ... logs <servicio>` dice por qué.

## 5. Encender y apagar la VM (control del costo)

La VM cuesta mientras esté encendida: **se desasigna fuera de las pruebas y la demo**.

```bash
az vm start      -g fitstore-rg -n fitstore-vm     # arranque completo: unos 130 s hasta todo healthy
az vm deallocate -g fitstore-rg -n fitstore-vm     # apagar y dejar de pagar cómputo (no uses "stop" ni cortes la luz)
```

Al encenderla, los contenedores vuelven solos (`restart: unless-stopped`, CA-8-16) y la IP es la misma (estática), por lo que DuckDNS no necesita actualizarse (CA-8-28). Hay una alerta de presupuesto de US$25 en Azure.

## 6. Herramientas internas por túnel SSH (CA-8-27)

Estas consolas escuchan solo en `127.0.0.1` de la VM; no están en internet.

```bash
ssh -N -L 16686:localhost:16686 -L 15672:localhost:15672 -L 8761:localhost:8761 -L 8025:localhost:8025 fitstore@57.156.71.26
```

| Herramienta | Dirección local | Notas |
|---|---|---|
| Jaeger (trazas) | http://localhost:16686 | |
| RabbitMQ | http://localhost:15672 | usuario y contraseña de `RABBITMQ_*` |
| Eureka | http://localhost:8761 | |
| Mailpit (correos de la demo) | http://localhost:8025 | todos los correos de la tienda llegan aquí |

**Logs:** `ssh fitstore@57.156.71.26`, luego `cd fitstore && docker compose -f infra/docker-compose.yml -f infra/docker-compose.cloud.yml logs -f --tail 100 order-service`.

**Seguir un pedido por su `X-Request-Id`:** el gateway genera un `X-Request-Id` por petición y lo devuelve en la respuesta. Haz la acción (por ejemplo, confirmar el checkout) con las herramientas del navegador abiertas, copia el `X-Request-Id` de la respuesta de `POST /api/v1/pedidos`, y en Jaeger busca por la etiqueta `request.id`/el trace asociado; verás la cadena gateway, order, catalog, payment y los consumidores de notification. En los logs: `docker compose ... logs | grep <id>`.

## 7. Respaldo y restauración (CA-8-26)

`infra/scripts/respaldo.sh` guarda cada día un único `AAAA-MM-DD.sql.gz` con las 7 bases (cada una con su `CREATE DATABASE`) y conserva los últimos 7.

Probar la restauración **sin tocar las bases reales**, en un PostgreSQL desechable:
```bash
docker run -d --name pg-prueba -e POSTGRES_PASSWORD=prueba postgres:17.10
sleep 8
gunzip -c /srv/fitstore/respaldos/2026-10-22.sql.gz | docker exec -i pg-prueba psql -U postgres -v ON_ERROR_STOP=0 >/dev/null
docker exec pg-prueba psql -U postgres -c '\l'            # deben aparecer las 7 bases
docker exec pg-prueba psql -U postgres -d order_db -c 'select count(*) from pedido'
docker rm -f pg-prueba
```
Para restaurar de verdad sobre la plataforma: detén los servicios de negocio, ejecuta el mismo `gunzip | psql` contra el contenedor `postgres` del compose (el respaldo usa `--clean --if-exists`, así que reemplaza las bases) y vuelve a levantar. Es una demo académica: no hay copia fuera de la VM; si se pierde todo, se vuelve a desplegar y el perfil `demo` recarga los datos.

## 8. Verificaciones desde fuera (CA-8-15, 16, 17, 18)

```bash
nmap -Pn -p 1-10000,15672,16686 57.156.71.26        # solo deben aparecer abiertos 22, 80 y 443
curl -I https://tienda.<DOMINIO>/                    # Strict-Transport-Security, X-Content-Type-Options, Referrer-Policy, Content-Security-Policy
curl -s -o /dev/null -w '%{http_code}\n' https://admin.<DOMINIO>/swagger-ui/index.html          # 401
curl -s -o /dev/null -w '%{http_code}\n' -u usuario:contraseña https://admin.<DOMINIO>/swagger-ui/index.html   # 200
```
En la VM: `docker compose ... ps` (todo *healthy*) y la suma de `mem_limit` (≈ 5,9 GiB, bajo los 6,5 GB):
`docker inspect -f '{{.HostConfig.Memory}}' $(docker compose ... ps -q) | paste -sd+ | bc`.

Si cambia la forma en que Angular genera el `index.html` (el script en línea del CSS diferido), el hash `sha256-…` de la CSP en `infra/caddy/Caddyfile` se recalcula con el índice compilado: `python3 -c` sobre el contenido de la etiqueta `<script>` (SHA-256 en base64).

## 9. Webhooks de las pasarelas (CA-8-23)

En el panel sandbox de Mercado Pago (y Culqi, si se usa), registra `https://tienda.<DOMINIO>/api/v1/pagos/webhooks/mercadopago` (y `/culqi`). Un pago de prueba debe confirmarse por webhook; se ve en los logs de `payment-service`.
