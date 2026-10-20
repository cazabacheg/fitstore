#!/usr/bin/env bash
# Pruebas de HU-0b-07 (SPEC-0b sección 7: "Script · npm run api:generate en CI").
# Uso: scripts/probar-api-generate.sh   (desde frontend/; necesita Docker)
set -euo pipefail
cd "$(dirname "$0")/.."

fallos=0
ok() { echo "  OK   $1"; }
falla() { echo "  FALLA $1"; fallos=$((fallos + 1)); }

servicios=(auth-service catalog-service delivery-service notification-service order-service payment-service settings-service)
salida="$(mktemp -d)"
trap 'rm -rf "$salida" "$salida-contratos"' EXIT

echo "# CA-0b-29 api:generate genera los clientes de los 7 servicios en shared/src/lib/api/<servicio>/"
SALIDA="$salida" bash scripts/api-generate.sh >/dev/null
for s in "${servicios[@]}"; do
  if [[ -f "$salida/$s/index.ts" && -d "$salida/$s/model" && -d "$salida/$s/api" ]]; then
    ok "CA-0b-29 $s"
  else
    falla "CA-0b-29 falta el cliente de $s"
  fi
done
cantidad="$(find "$salida" -mindepth 1 -maxdepth 1 -type d | wc -l)"
[[ "$cantidad" -eq 7 ]] && ok "CA-0b-29 son exactamente 7 servicios" || falla "CA-0b-29 se esperaban 7 carpetas y hay $cantidad"

echo "# CA-0b-30 con los contratos al día, la verificación pasa"
if bash scripts/verificar-clientes.sh >/dev/null 2>&1; then
  ok "CA-0b-30 clientes versionados al día"
else
  falla "CA-0b-30 la verificación falla aunque los clientes están al día"
fi

echo "# CA-0b-30 un contrato cambiado sin regenerar hace fallar la verificación y lo dice"
mkdir -p "$salida-contratos"
cp -r ../contracts/openapi "$salida-contratos/openapi"
sed -i '0,/operationId: /s//operationId: operacionCambiadaSinRegenerar_/' "$salida-contratos/openapi/auth-service.yaml"
if mensaje="$(CONTRATOS="$salida-contratos/openapi" bash scripts/verificar-clientes.sh 2>&1)"; then
  falla "CA-0b-30 la verificación pasó con un contrato modificado"
elif grep -q 'shared/src/lib/api está desactualizado' <<<"$mensaje"; then
  ok "CA-0b-30 falla indicando que shared/api está desactualizado"
else
  falla "CA-0b-30 falló pero sin el mensaje esperado: $mensaje"
fi

[[ "$fallos" -eq 0 ]] || { echo "$fallos prueba(s) fallaron"; exit 1; }
echo "Todo en verde"
