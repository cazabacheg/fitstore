#!/usr/bin/env bash
# Compara el contrato de un servicio con el OpenAPI que genera su código (SPEC-0a I8, CA-0a-22, contracts K2).
# 1) Corre ContratoOpenApiTest del servicio, que guarda GET /v3/api-docs en backend/<servicio>/target/openapi.json.
# 2) Corre oasdiff breaking entre contracts/openapi/<servicio>.yaml y ese archivo; falla con cambios incompatibles.
# Uso: scripts/contrato.sh auth-service
set -euo pipefail

servicio="${1:?Uso: scripts/contrato.sh <servicio>   (ej.: auth-service)}"
raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
contrato="$raiz/contracts/openapi/$servicio.yaml"
modulo="$raiz/backend/$servicio"
[[ -f "$contrato" ]] || { echo "No existe contracts/openapi/$servicio.yaml" >&2; exit 2; }
[[ -d "$modulo" ]] || { echo "No existe backend/$servicio" >&2; exit 2; }

cd "$raiz"
rm -f "$modulo/target/openapi.json"
./mvnw -B -q -pl "backend/$servicio" -am -Dtest=ContratoOpenApiTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Djacoco.skip=true test
[[ -s "$modulo/target/openapi.json" ]] || { echo "ContratoOpenApiTest no generó target/openapi.json" >&2; exit 1; }

# El contrato referencia comun.yaml: se monta todo contracts/ (solo lectura) junto al OpenAPI generado.
docker run --rm -v "$raiz/contracts:/contracts:ro" -v "$modulo/target:/generado:ro" tufin/oasdiff:v1.33.0 \
  breaking "/contracts/openapi/$servicio.yaml" /generado/openapi.json --fail-on ERR
