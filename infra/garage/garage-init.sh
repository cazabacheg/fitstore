#!/bin/sh
# Inicialización de Garage (SPEC-0a I5): asigna el layout de un nodo, importa la clave de acceso y crea
# los dos buckets la primera vez; deja el público con lectura anónima (endpoint web, puerto 3902).
# Es idempotente: se puede ejecutar de nuevo sin efectos. Usa la API de administración (v2) con curl.
#
# Variables: GARAGE_ADMIN_URL (por defecto http://garage:3903), GARAGE_ADMIN_TOKEN, S3_ACCESS_KEY,
#            S3_SECRET_KEY, BUCKET_PUBLICO, BUCKET_PRIVADO.
set -eu

URL="${GARAGE_ADMIN_URL:-http://garage:3903}/v2"
: "${GARAGE_ADMIN_TOKEN:?Falta GARAGE_ADMIN_TOKEN}"
: "${S3_ACCESS_KEY:?Falta S3_ACCESS_KEY}"
: "${S3_SECRET_KEY:?Falta S3_SECRET_KEY}"
BUCKET_PUBLICO="${BUCKET_PUBLICO:-fitstore-media}"
BUCKET_PRIVADO="${BUCKET_PRIVADO:-fitstore-privado}"

# llamar MÉTODO RUTA [CUERPO]: imprime el cuerpo en /tmp/respuesta y devuelve el código HTTP.
llamar() {
  curl -s -o /tmp/respuesta -w '%{http_code}' -X "$1" \
    -H "Authorization: Bearer ${GARAGE_ADMIN_TOKEN}" -H 'Content-Type: application/json' \
    ${3:+-d "$3"} "${URL}$2"
}

# Acepta 200 y 409 (ya existía); cualquier otro código detiene la inicialización.
exigir() {
  codigo="$(llamar "$@")"
  case "$codigo" in
    200|409) ;;
    *) echo "garage-init: $1 $2 devolvió HTTP $codigo" >&2; cat /tmp/respuesta >&2; exit 1 ;;
  esac
}

id_primero() { sed -n 's/.*"id": *"\([0-9a-f]\{64\}\)".*/\1/p' /tmp/respuesta | head -n1; }

echo "garage-init: esperando a Garage..."
i=0
until [ "$(llamar GET /GetClusterStatus)" = "200" ]; do
  i=$((i + 1)); [ "$i" -lt 60 ] || { echo "garage-init: Garage no respondió" >&2; exit 1; }
  sleep 2
done

if grep -q '"layoutVersion": *0' /tmp/respuesta; then
  nodo="$(id_primero)"
  echo "garage-init: asignando layout al nodo ${nodo}"
  exigir POST /UpdateClusterLayout \
    "{\"roles\":[{\"id\":\"${nodo}\",\"zone\":\"dc1\",\"capacity\":1073741824,\"tags\":[]}]}"
  exigir POST /ApplyClusterLayout '{"version":1}'
fi

exigir POST /ImportKey \
  "{\"accessKeyId\":\"${S3_ACCESS_KEY}\",\"secretAccessKey\":\"${S3_SECRET_KEY}\",\"name\":\"fitstore\"}"

for bucket in "$BUCKET_PUBLICO" "$BUCKET_PRIVADO"; do
  exigir POST /CreateBucket "{\"globalAlias\":\"${bucket}\"}"
  exigir GET "/GetBucketInfo?globalAlias=${bucket}"
  id="$(id_primero)"
  exigir POST /AllowBucketKey \
    "{\"bucketId\":\"${id}\",\"accessKeyId\":\"${S3_ACCESS_KEY}\",\"permissions\":{\"read\":true,\"write\":true,\"owner\":true}}"
  if [ "$bucket" = "$BUCKET_PUBLICO" ]; then
    exigir POST "/UpdateBucket?id=${id}" '{"websiteAccess":{"enabled":true,"indexDocument":"index.html"}}'
  fi
  echo "garage-init: bucket ${bucket} listo"
done
echo "garage-init: terminado"
