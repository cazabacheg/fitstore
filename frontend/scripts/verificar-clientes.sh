#!/usr/bin/env bash
# CA-0b-30: regenera los clientes en una carpeta temporal y falla si difieren de los versionados en Git.
# Corre en el job `frontend` de ci.yml. Variable opcional: CONTRATOS (carpeta de los YAML).
set -euo pipefail

raiz="$(cd "$(dirname "$0")/.." && pwd)"
versionados="$raiz/projects/shared/src/lib/api"
temporal="$(mktemp -d)"
trap 'rm -rf "$temporal"' EXIT

SALIDA="$temporal" bash "$raiz/scripts/api-generate.sh" >/dev/null

if ! diff -r -q "$versionados" "$temporal" >/dev/null 2>&1; then
  echo "frontend/projects/shared/src/lib/api está desactualizado respecto a contracts/openapi." >&2
  echo "Ejecuta 'npm run api:generate' en frontend/ y versiona el resultado. Diferencias:" >&2
  diff -r -q "$versionados" "$temporal" >&2 || true
  exit 1
fi
echo "Los clientes de API están al día."
