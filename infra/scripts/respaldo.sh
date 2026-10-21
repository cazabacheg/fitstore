#!/usr/bin/env bash
# Respaldo diario de las 7 bases de PostgreSQL (SPEC-8 I9, CA-8-26). Corre por cron en la VM, no es un contenedor.
# Genera RESPALDOS_DIR/AAAA-MM-DD.sql.gz (un solo archivo con las 7 bases, cada una con su CREATE DATABASE) y
# conserva los últimos 7. Para restaurar, ver docs/despliegue.md.
#
# Uso:  infra/scripts/respaldo.sh
# Variables opcionales:
#   RESPALDOS_DIR  carpeta de destino (por defecto /srv/fitstore/respaldos)
#   CONSERVAR      cuántos respaldos guardar (por defecto 7)
#   COMPOSE_ARCHIVOS  archivos compose (por defecto base + cloud)
# Cron sugerido (crontab -e del usuario fitstore):  0 3 * * * /home/fitstore/fitstore/infra/scripts/respaldo.sh >> /home/fitstore/respaldo.log 2>&1
set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DESTINO="${RESPALDOS_DIR:-/srv/fitstore/respaldos}"
CONSERVAR="${CONSERVAR:-7}"
BASES=(auth_db catalog_db settings_db order_db payment_db delivery_db notification_db)
COMPOSE=(docker compose -f "$RAIZ/infra/docker-compose.yml" -f "$RAIZ/infra/docker-compose.cloud.yml")
if [[ -n "${COMPOSE_ARCHIVOS:-}" ]]; then read -r -a COMPOSE <<<"docker compose ${COMPOSE_ARCHIVOS}"; fi

mkdir -p "$DESTINO"
archivo="$DESTINO/$(date +%F).sql.gz"
temporal="$archivo.parcial"
trap 'rm -f "$temporal"' EXIT

# Cada base con --create para que se pueda restaurar sobre un servidor vacío o en una base de prueba (renombrando)
for base in "${BASES[@]}"; do
  "${COMPOSE[@]}" exec -T postgres pg_dump -U postgres --create --clean --if-exists "$base"
done | gzip -9 > "$temporal"

# Un respaldo vacío o corrupto no debe reemplazar al bueno del día
gzip -t "$temporal"
if [[ "$(gzip -dc "$temporal" | grep -c '^CREATE DATABASE')" -ne "${#BASES[@]}" ]]; then
  echo "El respaldo no contiene las ${#BASES[@]} bases; no se guarda" >&2
  exit 1
fi
mv "$temporal" "$archivo"
echo "$(date -Is) respaldo guardado en $archivo ($(du -h "$archivo" | cut -f1))"

# Rotación: se conservan los últimos $CONSERVAR
ls -1t "$DESTINO"/*.sql.gz 2>/dev/null | tail -n +"$((CONSERVAR + 1))" | xargs -r rm -f --
