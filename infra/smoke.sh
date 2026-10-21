#!/usr/bin/env bash
# Prueba de humo de la plataforma completa de dev (SPEC-0a, infraestructura 9).
# Construye las imágenes, levanta el compose de dev, espera a que todo quede healthy y verifica los criterios
# de humo (CA-0a-02, 03, 05, 07, 17 y 19). Termina con código 0 solo si TODAS las verificaciones pasan.
#
# Uso (desde cualquier carpeta):  infra/smoke.sh
# Variables opcionales:
#   SMOKE_TIMEOUT        segundos máximos desde "up" hasta todo healthy (por defecto 300 = CA-0a-02)
#   SMOKE_REGISTRO       segundos máximos para que Eureka liste los 8 servicios (por defecto 120)
#   SMOKE_SIN_ARRANQUE=1 no construye ni levanta: verifica una plataforma que ya está corriendo
#   SMOKE_COMPOSE_EXTRA  archivo compose adicional (ej.: cambiar un puerto publicado si el de la máquina está ocupado)
#   SMOKE_BAJAR=1        al terminar, baja el compose y borra sus volúmenes (lo usa stage.yml)
# Requisitos: docker con compose, curl, jq.
set -uo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INFRA="$RAIZ/infra"
COMPOSE=(docker compose -f "$INFRA/docker-compose.yml" -f "$INFRA/docker-compose.dev.yml")
if [[ -n "${SMOKE_COMPOSE_EXTRA:-}" ]]; then COMPOSE+=(-f "$SMOKE_COMPOSE_EXTRA"); fi
TIMEOUT="${SMOKE_TIMEOUT:-300}"
REGISTRO="${SMOKE_REGISTRO:-120}"

# nombre-del-servicio puerto base-de-datos prefijo-de-variable
SERVICIOS=(
  "auth-service 8081 auth_db AUTH"
  "catalog-service 8082 catalog_db CATALOG"
  "order-service 8083 order_db ORDER"
  "payment-service 8084 payment_db PAYMENT"
  "delivery-service 8085 delivery_db DELIVERY"
  "notification-service 8086 notification_db NOTIFICATION"
  "settings-service 8087 settings_db SETTINGS"
)

FALLOS=0
fallo() { echo "  FALLA: $*" >&2; FALLOS=$((FALLOS + 1)); }
ok() { echo "  ok: $*"; }
seccion() { echo; echo "== $* =="; }

command -v jq >/dev/null || { echo "Falta jq" >&2; exit 2; }
command -v curl >/dev/null || { echo "Falta curl" >&2; exit 2; }

# Variables del entorno: se crea infra/.env desde .env.example solo si no existe (nunca se pisa uno propio).
if [[ ! -f "$INFRA/.env" ]]; then
  cp "$INFRA/.env.example" "$INFRA/.env"
  echo "Creado infra/.env desde .env.example (valores solo de desarrollo)"
fi
set -a
# shellcheck disable=SC1091
# Si una línea no es válida en bash (p. ej. un valor con espacios o < > sin comillas), se detiene aquí: seguir sin las
# variables hacía fallar después verificaciones que no tenían la culpa.
. "$INFRA/.env" || { echo "No se pudo cargar infra/.env: revisa las comillas de los valores con espacios" >&2; exit 2; }
set +a

limpiar() {
  if [[ "${SMOKE_BAJAR:-0}" == "1" ]]; then
    echo; echo "Bajando la plataforma (SMOKE_BAJAR=1)"
    "${COMPOSE[@]}" down -v --remove-orphans >/dev/null 2>&1
  fi
}
trap limpiar EXIT

# Estado de todos los contenedores del proyecto: una línea "servicio|estado|salud|códigoSalida" por contenedor.
estado_contenedores() {
  "${COMPOSE[@]}" ps -a --format json | jq -r -s '
    (if (.[0]|type)=="array" then .[0] else . end)[] | "\(.Service)|\(.State)|\(.Health // "")|\(.ExitCode)"'
}

# Todo healthy: los que tienen healthcheck en "healthy", los demás corriendo, y los de un solo uso terminados con 0.
pendientes() {
  estado_contenedores | awk -F'|' '
    $2=="running" && ($3=="healthy" || $3=="") {next}
    $2=="exited" && $4==0 {next}
    {print $1 " (" $2 (($3!="")?"/"$3:"") (($2=="exited")?" código " $4:"") ")"}'
}

# --- Arranque ---------------------------------------------------------------------------------------------
if [[ "${SMOKE_SIN_ARRANQUE:-0}" != "1" ]]; then
  seccion "Construcción de las imágenes (no cuenta para los 5 minutos de CA-0a-02)"
  t0=$SECONDS
  if "${COMPOSE[@]}" build; then
    echo "Tiempo de build: $((SECONDS - t0)) s"
  else
    echo "FALLA: docker compose build" >&2; exit 1
  fi
  seccion "Arranque"
fi
# El cronómetro de CA-0a-02 empieza en "up": compose espera la salud de las dependencias dentro del propio "up -d"
inicio=$SECONDS
if [[ "${SMOKE_SIN_ARRANQUE:-0}" != "1" ]]; then
  timeout "$TIMEOUT" "${COMPOSE[@]}" up -d || echo "FALLA: docker compose up -d no terminó bien en ${TIMEOUT} s" >&2
fi

# CA-0a-02 Arranque completo: en menos de 5 minutos todos los contenedores están healthy
seccion "CA-0a-02 Arranque completo (máximo ${TIMEOUT} s)"
listos=false
while (( SECONDS - inicio <= TIMEOUT )); do
  if [[ -z "$(pendientes)" ]]; then listos=true; break; fi
  sleep 5
done
tarda=$((SECONDS - inicio))
if $listos; then
  ok "todos los contenedores healthy en ${tarda} s (límite ${TIMEOUT} s)"
else
  fallo "tras ${TIMEOUT} s no están healthy: $(pendientes | paste -sd ',' -)"
  for c in $(pendientes | awk '{print $1}'); do
    echo "--- últimos logs de $c ---" >&2
    "${COMPOSE[@]}" logs --tail 30 "$c" >&2 2>&1
  done
fi

# CA-0a-03 Registro en Eureka: api-gateway y los 7 servicios de negocio con estado UP
seccion "CA-0a-03 Registro en Eureka"
esperados=("API-GATEWAY")
for s in "${SERVICIOS[@]}"; do read -r nombre _ <<<"$s"; esperados+=("${nombre^^}"); done
registro_ok=false
limite=$((SECONDS + REGISTRO))
while true; do
  apps="$(curl -fsS -H 'Accept: application/json' http://localhost:8761/eureka/apps 2>/dev/null || true)"
  sin_up=()
  for app in "${esperados[@]}"; do
    n="$(jq -r --arg a "$app" '[.applications.application[]? | select(.name==$a) | .instance[]? | select(.status=="UP")] | length' <<<"${apps:-{\}}" 2>/dev/null || echo 0)"
    [[ "${n:-0}" -ge 1 ]] || sin_up+=("$app")
  done
  if [[ ${#sin_up[@]} -eq 0 ]]; then registro_ok=true; break; fi
  (( SECONDS >= limite )) && break
  sleep 5
done
if $registro_ok; then ok "Eureka lista ${#esperados[@]} aplicaciones en UP: ${esperados[*]}"
else fallo "Eureka no lista en UP: ${sin_up[*]}"; fi

# CA-0a-05 Esquema migrado y validado: V1 (y sus semillas) aplicadas en cada base, con Hibernate en validate
seccion "CA-0a-05 Esquema migrado y validado"
for s in "${SERVICIOS[@]}"; do
  read -r nombre puerto base prefijo <<<"$s"
  usuario_var="${prefijo}_DB_USER"; clave_var="${prefijo}_DB_PASSWORD"
  esperadas="$(ls "$RAIZ/backend/$nombre/src/main/resources/db/migration/" 2>/dev/null \
    | sed -nE 's/^V([0-9_]+)__.*\.sql$/\1/p' | tr '_' '.' | sort -V | paste -sd ' ' -)"
  aplicadas="$("${COMPOSE[@]}" exec -T -e PGPASSWORD="${!clave_var}" postgres \
    psql -h 127.0.0.1 -U "${!usuario_var}" -d "$base" -At \
    -c "SELECT version FROM flyway_schema_history WHERE success AND version IS NOT NULL" 2>/dev/null \
    | sort -V | paste -sd ' ' -)"
  fallidas="$("${COMPOSE[@]}" exec -T -e PGPASSWORD="${!clave_var}" postgres \
    psql -h 127.0.0.1 -U "${!usuario_var}" -d "$base" -At \
    -c "SELECT count(*) FROM flyway_schema_history WHERE NOT success" 2>/dev/null)"
  if [[ -n "$esperadas" && "$aplicadas" == "$esperadas" && "${fallidas:-x}" == "0" ]]; then
    ok "$nombre: Flyway aplicó V[$aplicadas] en $base"
  else
    fallo "$nombre: en $base se esperaban las migraciones [$esperadas] y están aplicadas [$aplicadas] (fallidas: ${fallidas:-?})"
  fi
  # Hibernate en validate (la configuración que entrega el config-server) y el servicio sano con esa base
  ddl="$(curl -fsS -u "$CONFIG_USER:$CONFIG_PASSWORD" "http://localhost:8888/$nombre/dev" 2>/dev/null \
    | jq -r '[.propertySources[].source["spring.jpa.hibernate.ddl-auto"]?] | map(select(. != null)) | first // "ninguno"' 2>/dev/null)"
  salud="$(curl -fsS "http://localhost:$puerto/actuator/health" 2>/dev/null | jq -r .status 2>/dev/null)"
  if [[ "$ddl" == "validate" && "$salud" == "UP" ]]; then
    ok "$nombre: ddl-auto=validate y /actuator/health UP"
  else
    fallo "$nombre: ddl-auto='${ddl:-?}' (esperado validate), /actuator/health='${salud:-sin respuesta}' (esperado UP)"
  fi
done

# CA-0a-07 Aislamiento de datos: el usuario de un servicio no puede leer la base de otro
seccion "CA-0a-07 Aislamiento de datos"
for s in "${SERVICIOS[@]}"; do
  read -r nombre _ _ prefijo <<<"$s"
  usuario_var="${prefijo}_DB_USER"; clave_var="${prefijo}_DB_PASSWORD"
  for otro in "${SERVICIOS[@]}"; do
    read -r otro_nombre _ otra_base _ <<<"$otro"
    [[ "$otro_nombre" == "$nombre" ]] && continue
    if "${COMPOSE[@]}" exec -T -e PGPASSWORD="${!clave_var}" postgres \
        psql -h 127.0.0.1 -U "${!usuario_var}" -d "$otra_base" -At -c "SELECT 1" >/dev/null 2>&1; then
      fallo "el usuario de $nombre SÍ pudo conectarse a $otra_base"
    fi
  done
  ok "el usuario de $nombre fue rechazado en las otras 6 bases"
done
# Control positivo: cada usuario sí entra a su propia base (sin esto, un error de credenciales daría un falso ok)
for s in "${SERVICIOS[@]}"; do
  read -r nombre _ base prefijo <<<"$s"
  usuario_var="${prefijo}_DB_USER"; clave_var="${prefijo}_DB_PASSWORD"
  "${COMPOSE[@]}" exec -T -e PGPASSWORD="${!clave_var}" postgres \
    psql -h 127.0.0.1 -U "${!usuario_var}" -d "$base" -At -c "SELECT 1" >/dev/null 2>&1 \
    || fallo "control: el usuario de $nombre no puede entrar a su propia base $base"
done

# CA-0a-17 Swagger agregado: /swagger-ui del gateway ofrece los 7 servicios y cada documentación responde
seccion "CA-0a-17 Swagger agregado"
codigo="$(curl -s -o /dev/null -w '%{http_code}' -L http://localhost:8080/swagger-ui)"
[[ "$codigo" == "200" ]] && ok "GET /swagger-ui -> 200" || fallo "GET /swagger-ui -> $codigo"
config="$(curl -fsS http://localhost:8080/v3/api-docs/swagger-config 2>/dev/null || true)"
for s in "${SERVICIOS[@]}"; do
  read -r nombre _ <<<"$s"
  url="$(jq -r --arg n "$nombre" '[.urls[]? | select(.name==$n)] | first | .url // empty' <<<"${config:-{\}}" 2>/dev/null)"
  if [[ "$url" != "/$nombre/v3/api-docs" ]]; then
    fallo "el selector de Swagger no ofrece $nombre (url: ${url:-ninguna})"; continue
  fi
  version="$(curl -fsS "http://localhost:8080$url" 2>/dev/null | jq -r .openapi 2>/dev/null)"
  if [[ "$version" == 3.0* ]]; then ok "$nombre ofrecido en el selector y $url responde OpenAPI $version"
  else fallo "$nombre: GET $url no devolvió un OpenAPI 3.0 (openapi=${version:-sin respuesta})"; fi
done
total_urls="$(jq -r '.urls | length' <<<"${config:-{\}}" 2>/dev/null || echo 0)"
[[ "$total_urls" == "7" ]] && ok "el selector tiene exactamente 7 servicios" || fallo "el selector tiene $total_urls servicios (se esperaban 7)"

# CA-0a-19 Memoria: cada contenedor tiene mem_limit y la suma no supera 6,5 GB (RNF-03)
seccion "CA-0a-19 Memoria"
LIMITE_TOTAL=6500000000
suma=0
while read -r nombre memoria; do
  [[ -z "$nombre" ]] && continue
  if [[ "${memoria:-0}" -le 0 ]]; then fallo "$nombre no tiene mem_limit"; else suma=$((suma + memoria)); fi
done < <("${COMPOSE[@]}" ps -a -q | xargs docker inspect -f '{{.Name}} {{.HostConfig.Memory}}')
if (( suma > 0 && suma <= LIMITE_TOTAL )); then
  ok "suma de mem_limit: $((suma / 1048576)) MiB (límite 6,5 GB = $((LIMITE_TOTAL / 1048576)) MiB)"
else
  fallo "la suma de mem_limit es $((suma / 1048576)) MiB y el máximo es 6,5 GB ($((LIMITE_TOTAL / 1048576)) MiB)"
fi
echo "  uso real en reposo (docker stats):"
docker stats --no-stream --format '    {{.Name}}: {{.MemUsage}} ({{.MemPerc}})' $("${COMPOSE[@]}" ps -q) 2>/dev/null

# --- Resultado --------------------------------------------------------------------------------------------
echo
if (( FALLOS == 0 )); then
  echo "SMOKE OK: todas las verificaciones pasaron (arranque en ${tarda} s)"
  exit 0
fi
echo "SMOKE FALLÓ: $FALLOS verificación(es) con error" >&2
exit 1
