#!/usr/bin/env bash
# Verifica que cada criterio de aceptación de una spec tenga al menos una prueba o verificación
# que lleve su id (estrategia multiagente V1). Uso: scripts/ca-cobertura.sh 0a
set -euo pipefail

spec="${1:?Uso: scripts/ca-cobertura.sh <spec>   (ej.: 0a, 2)}"
raiz="$(git rev-parse --show-toplevel)"
archivo="$(ls "$raiz"/docs/specs/SPEC-"$spec"-*.md 2>/dev/null | head -n1)"
[[ -n "$archivo" ]] || { echo "No existe docs/specs/SPEC-$spec-*.md" >&2; exit 2; }

# Criterios definidos en la spec (líneas que empiezan con el id, dentro de los bloques gherkin).
mapfile -t criterios < <(grep -oE "^CA-$spec-[0-9]+b?" "$archivo" | sort -u -V)
[[ ${#criterios[@]} -gt 0 ]] || { echo "La spec no define criterios CA-$spec-xx" >&2; exit 2; }

# Dónde se buscan: todo el repo salvo documentación, contratos y dependencias.
faltan=()
for ca in "${criterios[@]}"; do
  if ! git -C "$raiz" grep -qwF -- "$ca" -- . ':!docs' ':!contracts' ':!CLAUDE.md' ':!**/node_modules/**'; then
    faltan+=("$ca")
  fi
done

total=${#criterios[@]}
echo "SPEC-$spec: $((total - ${#faltan[@]})) de $total criterios con prueba"
if [[ ${#faltan[@]} -gt 0 ]]; then
  printf 'Sin prueba: %s\n' "${faltan[@]}"
  exit 1
fi
