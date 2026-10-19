#!/usr/bin/env bash
# Lista los archivos que una rama cambió fuera de su alcance (estrategia multiagente V4).
# El alcance es un archivo con un patrón de ruta por línea (estilo .gitignore: `backend/auth-service/**`);
# las líneas vacías y las que empiezan con # se ignoran.
# Uso: scripts/fuera-de-alcance.sh docs/implementacion/briefs/SPEC-0a.1.alcance [base]   (base por defecto: dev)
set -euo pipefail

alcance="${1:?Uso: scripts/fuera-de-alcance.sh <archivo .alcance> [base]}"
base="${2:-dev}"
[[ -f "$alcance" ]] || { echo "No existe $alcance" >&2; exit 2; }

mapfile -t patrones < <(grep -vE '^\s*(#|$)' "$alcance")
fuera=()
while IFS= read -r archivo; do
  dentro=false
  for p in "${patrones[@]}"; do
    # shellcheck disable=SC2053
    if [[ "$archivo" == $p ]]; then dentro=true; break; fi
  done
  $dentro || fuera+=("$archivo")
done < <(git diff --name-only "$base"...HEAD)

if [[ ${#fuera[@]} -gt 0 ]]; then
  echo "Archivos fuera del alcance de $(basename "$alcance"):"
  printf '  %s\n' "${fuera[@]}"
  exit 1
fi
echo "Todo dentro del alcance ($(git diff --name-only "$base"...HEAD | wc -l) archivos)."
