#!/usr/bin/env bash
# Genera los clientes TypeScript (typescript-angular) de los 7 servicios desde contracts/openapi/*-service.yaml
# con OpenAPI Generator v7.10.0 (SPEC-0b R5, CA-0b-29). Los clientes se versionan en Git y nunca se editan a mano.
# Variables opcionales: SALIDA (carpeta destino) y CONTRATOS (carpeta de los YAML), útiles para las pruebas y el CI.
set -euo pipefail

raiz="$(cd "$(dirname "$0")/.." && pwd)"
contratos="$(cd "${CONTRATOS:-$raiz/../contracts/openapi}" && pwd)"
mkdir -p "${SALIDA:-$raiz/projects/shared/src/lib/api}"
salida="$(cd "${SALIDA:-$raiz/projects/shared/src/lib/api}" && pwd)"
imagen="openapitools/openapi-generator-cli:v7.10.0"

# Se regenera desde cero para que un endpoint quitado del contrato también desaparezca del cliente.
find "$salida" -mindepth 1 -maxdepth 1 -exec rm -rf {} +

for yaml in "$contratos"/*-service.yaml; do
  servicio="$(basename "$yaml" .yaml)"
  echo "Generando $servicio"
  docker run --rm -u "$(id -u):$(id -g)" \
    -v "$contratos:/contratos:ro" -v "$salida:/salida" \
    "$imagen" generate -g typescript-angular \
    -i "/contratos/$servicio.yaml" -o "/salida/$servicio" \
    >/dev/null
  # Archivos del generador que no se versionan: solo se quedan api/, model/ y los de configuración del cliente.
  rm -rf "$salida/$servicio/.openapi-generator" "$salida/$servicio/git_push.sh" "$salida/$servicio/README.md" \
    "$salida/$servicio/.gitignore" "$salida/$servicio/.openapi-generator-ignore"
done
echo "Clientes generados en $salida"
