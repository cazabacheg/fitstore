#!/usr/bin/env bash
# Despliegue de FitStore en la VM de la nube (SPEC-8, CA-8-20 a CA-8-22). Lo usa deploy.yml y también se puede correr
# a mano desde una copia del repositorio. Las imágenes se construyen EN LA VM (desvío acordado: sin GHCR).
#
# Pasos:
#   1. copia el código de la referencia pedida a la VM por SSH (sin tocar infra/.env ni las claves),
#   2. escribe la etiqueta en el .env de la VM (FITSTORE_TAG) y crea una vez las claves JWT fuera del repositorio,
#   3. docker compose build (de a uno, para no agotar la memoria de la VM) y up -d,
#   4. espera a que todo quede healthy y verifica por HTTPS (páginas, /api, cabeceras y Swagger con contraseña).
#
# Uso:  infra/scripts/desplegar.sh [referencia-git]      (por defecto HEAD)
# Variables:
#   VM_HOST (obligatoria)  IP o nombre de la VM        VM_USER (por defecto fitstore)
#   VM_DIR                 carpeta en la VM (por defecto fitstore, relativa al HOME)
#   SSH_OPCIONES           opciones extra de ssh (p. ej. "-i ~/.ssh/clave -o UserKnownHostsFile=...")
#   ESPERA_SEGUNDOS        máximo para el healthy (por defecto 600)
#   SIN_VERIFICAR=1        omite la verificación HTTPS (cuando el DNS todavía no apunta a la VM)
set -euo pipefail

: "${VM_HOST:?Falta VM_HOST}"
VM_USER="${VM_USER:-fitstore}"
VM_DIR="${VM_DIR:-fitstore}"
REF="${1:-HEAD}"
ESPERA="${ESPERA_SEGUNDOS:-600}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck disable=SC2206
SSH=(ssh -o BatchMode=yes -o ServerAliveInterval=30 ${SSH_OPCIONES:-} "$VM_USER@$VM_HOST")

ETIQUETA="$(git -C "$RAIZ" rev-parse --short=12 "$REF")"
echo "== Desplegando $REF (etiqueta $ETIQUETA) en $VM_USER@$VM_HOST:~/$VM_DIR"

echo "== 1. Copiando el código"
git -C "$RAIZ" archive --format=tar "$REF" | "${SSH[@]}" \
  "set -e; rm -rf ~/$VM_DIR.nuevo; mkdir -p ~/$VM_DIR.nuevo ~/$VM_DIR; tar -x -C ~/$VM_DIR.nuevo;
   rsync -a --delete --exclude 'infra/.env' ~/$VM_DIR.nuevo/ ~/$VM_DIR/; rm -rf ~/$VM_DIR.nuevo"

echo "== 2-3. Etiqueta, claves, construcción y arranque"
"${SSH[@]}" "ETIQUETA='$ETIQUETA' VM_DIR='$VM_DIR' ESPERA='$ESPERA' bash -s" <<'REMOTO'
set -euo pipefail
cd ~/"$VM_DIR"
ENV=infra/.env
[[ -f "$ENV" ]] || { echo "Falta ~/$VM_DIR/$ENV: créalo antes (ver docs/despliegue.md)" >&2; exit 1; }
# La etiqueta del commit desplegado queda en el .env (para saber qué corre y poder volver atrás)
grep -v '^FITSTORE_TAG=' "$ENV" > "$ENV.tmp" || true
echo "FITSTORE_TAG=$ETIQUETA" >> "$ENV.tmp"
mv "$ENV.tmp" "$ENV"
chmod 600 "$ENV"

# Claves JWT de auth: se crean una sola vez, fuera del repositorio (rsync --delete no las toca)
JWT_DIR="$(grep '^JWT_DIR=' "$ENV" | cut -d= -f2- | tr -d "'\"")"
[[ -n "$JWT_DIR" ]] || { echo "Falta JWT_DIR en $ENV" >&2; exit 1; }
if [[ ! -f "$JWT_DIR/privada.pem" ]]; then
  mkdir -p "$JWT_DIR"
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$JWT_DIR/privada.pem"
  openssl rsa -in "$JWT_DIR/privada.pem" -pubout -out "$JWT_DIR/publica.pem"
  chmod 755 "$JWT_DIR"; chmod 644 "$JWT_DIR"/*.pem   # el usuario del contenedor no es el de la VM
  echo "Claves JWT creadas en $JWT_DIR"
fi

COMPOSE=(docker compose -f infra/docker-compose.yml -f infra/docker-compose.cloud.yml)
COMPOSE_PARALLEL_LIMIT=1 "${COMPOSE[@]}" build
"${COMPOSE[@]}" up -d --remove-orphans

echo "Esperando que todo quede healthy (máximo $ESPERA s)"
inicio=$SECONDS
while true; do
  pendientes="$("${COMPOSE[@]}" ps -a --format json | jq -r -s '
    (if (.[0]|type)=="array" then .[0] else . end)[] |
    select(((.State=="running") and ((.Health // "")=="" or .Health=="healthy")) or (.State=="exited" and .ExitCode==0) | not) |
    "\(.Service) (\(.State)/\(.Health // ""))"')"
  [[ -z "$pendientes" ]] && break
  if (( SECONDS - inicio > ESPERA )); then
    echo "No quedaron healthy: $pendientes" >&2
    "${COMPOSE[@]}" logs --tail 30 >&2 || true
    exit 1
  fi
  sleep 10
done
echo "Todo healthy en $((SECONDS - inicio)) s"
docker image prune -f >/dev/null
REMOTO

if [[ "${SIN_VERIFICAR:-0}" == "1" ]]; then
  echo "== Verificación HTTPS omitida (SIN_VERIFICAR=1)"
  exit 0
fi

echo "== 4. Verificación por HTTPS"
DOMINIO="$("${SSH[@]}" "grep '^DOMINIO=' ~/$VM_DIR/infra/.env | cut -d= -f2- | tr -d \"'\\\"\"")"
SWAGGER_USER="$("${SSH[@]}" "grep '^SWAGGER_USER=' ~/$VM_DIR/infra/.env | cut -d= -f2- | tr -d \"'\\\"\"")"
TIENDA="https://tienda.$DOMINIO"
ADMIN="https://admin.$DOMINIO"
FALLOS=0
verificar() { # descripción, comando...
  local d="$1"; shift
  if "$@" >/dev/null 2>&1; then echo "  ok: $d"; else echo "  FALLA: $d" >&2; FALLOS=$((FALLOS + 1)); fi
}
codigo() { curl -s -o /dev/null -w '%{http_code}' --max-time 20 "$@"; }
esperar_https() { for _ in $(seq 1 30); do [[ "$(codigo "$1")" == "200" ]] && return 0; sleep 10; done; return 1; }

verificar "CA-8-14 la tienda responde 200 con certificado válido" esperar_https "$TIENDA/"
verificar "CA-8-14 el backoffice responde 200 con certificado válido" esperar_https "$ADMIN/"
verificar "CA-8-14 /api llega al gateway (catálogo público)" bash -c "[[ \"\$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 '$TIENDA/api/v1/productos?size=1')\" == 200 ]]"
verificar "CA-8-17 HSTS, nosniff, Referrer-Policy y CSP presentes" bash -c "
  h=\$(curl -sI --max-time 20 '$TIENDA/'); grep -qi '^strict-transport-security' <<<\"\$h\" &&
  grep -qi '^x-content-type-options: nosniff' <<<\"\$h\" && grep -qi '^referrer-policy' <<<\"\$h\" &&
  grep -qi '^content-security-policy' <<<\"\$h\""
verificar "CA-8-18 /swagger-ui pide contraseña (401 sin credenciales)" bash -c "[[ \"\$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 '$ADMIN/swagger-ui/index.html')\" == 401 ]]"
if [[ -n "${SWAGGER_PASSWORD:-}" ]]; then
  verificar "CA-8-18 /swagger-ui abre con usuario y contraseña" bash -c "[[ \"\$(curl -s -o /dev/null -w '%{http_code}' --max-time 20 -u '$SWAGGER_USER:$SWAGGER_PASSWORD' '$ADMIN/swagger-ui/index.html')\" == 200 ]]"
fi
# --- Verificaciones que se hacen dentro de la VM ---
remoto() { "${SSH[@]}" "VM_DIR='$VM_DIR' bash -s"; }
# CA-8-16 La suma de mem_limit es ≤ 6,5 GB y todo reinicia solo (restart: unless-stopped)
verificar "CA-8-16 suma de mem_limit ≤ 6,5 GB y restart unless-stopped" remoto <<'REMOTO'
set -e
cd ~/"$VM_DIR"
ids=$(docker compose -f infra/docker-compose.yml -f infra/docker-compose.cloud.yml ps -q)
suma=$(docker inspect -f '{{.HostConfig.Memory}}' $ids | paste -sd+ | bc)
[ "$suma" -le 6500000000 ]
! docker inspect -f '{{.Name}} {{.HostConfig.RestartPolicy.Name}}' $ids | grep -v -E 'garage-init|unless-stopped'
REMOTO
# CA-8-25 El proveedor simulado queda disponible en la nube como respaldo
verificar "CA-8-25 PAGOS_SIMULADO_HABILITADO=true en la VM" "${SSH[@]}" "grep -q '^PAGOS_SIMULADO_HABILITADO=true' ~/$VM_DIR/infra/.env"
# CA-8-31 Las cuentas demo solo entran con DEMO_PASSWORD: la contraseña de desarrollo NO funciona en la nube
login_demo() { # contraseña -> código HTTP
  curl -s -o /dev/null -w '%{http_code}' --max-time 20 -X POST -H 'Content-Type: application/json' \
    -d "{\"email\":\"lucia.mendoza@fitstore.demo\",\"password\":\"$1\"}" "$TIENDA/api/v1/auth/login"
}
verificar "CA-8-31 la contraseña de desarrollo (FitStoreDemo2026) es rechazada" bash -c "[[ \"$(login_demo FitStoreDemo2026)\" == 401 ]]"
DEMO_PASSWORD_VM="$("${SSH[@]}" "grep '^DEMO_PASSWORD=' ~/$VM_DIR/infra/.env | cut -d= -f2-" | tr -d "'\"")"
if [[ -n "$DEMO_PASSWORD_VM" ]]; then
  verificar "CA-8-31 la cuenta demo entra con DEMO_PASSWORD" bash -c "[[ \"$(login_demo "$DEMO_PASSWORD_VM")\" == 200 ]]"
fi
if (( FALLOS > 0 )); then echo "Despliegue con $FALLOS verificación(es) fallida(s)" >&2; exit 1; fi
echo "== Despliegue correcto: $TIENDA · $ADMIN (etiqueta $ETIQUETA)"
