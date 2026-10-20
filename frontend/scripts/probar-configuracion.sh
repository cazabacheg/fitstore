#!/usr/bin/env bash
# Pruebas estáticas de la configuración del workspace (SPEC-0b sección 7): proxy, presupuestos y job `frontend` de CI.
# Uso (desde frontend/): scripts/probar-configuracion.sh
# Las pruebas dinámicas de CA-0b-01 y CA-0b-02 son los propios comandos: npm run build, npm run lint, npm test.
set -euo pipefail
cd "$(dirname "$0")/.."

fallos=0
ok() { echo "  OK   $1"; }
falla() { echo "  FALLA $1"; fallos=$((fallos + 1)); }
verificar() { # descripción, comando...
  local descripcion="$1"; shift
  if "$@" >/dev/null 2>&1; then ok "$descripcion"; else falla "$descripcion"; fi
}

ci="../.github/workflows/ci.yml"
trabajo_frontend="$(awk '/^  frontend:/{dentro=1;print;next} dentro&&/^  [a-z]/{exit} dentro{print}' "$ci")"

echo "# CA-0b-01 el build de las dos apps y de shared corre con presupuestos de tamaño"
verificar "CA-0b-01 npm run build compila shared, storefront y backoffice" \
  node -e "const s=require('./package.json').scripts.build; for (const p of ['shared','storefront','backoffice']) if(!s.includes('ng build '+p)) process.exit(1)"
verificar "CA-0b-01 presupuestos de tamaño en la configuración de producción de las dos apps" \
  node -e "const a=require('./angular.json').projects; for (const p of ['storefront','backoffice']) { const b=a[p].architect.build.configurations.production.budgets; if(!b.some(x=>x.type==='initial'&&x.maximumError)) process.exit(1) }"
verificar "CA-0b-01 el locale es es-PE en las dos apps" \
  node -e "const a=require('./angular.json').projects; for (const p of ['storefront','backoffice']) if(a[p].i18n.sourceLocale!=='es-PE') process.exit(1)"

echo "# CA-0b-02 lint y pruebas con umbral de 80 % de líneas por proyecto (sin shared/api)"
verificar "CA-0b-02 umbral de 80 % de líneas en shared, storefront y backoffice" \
  node -e "const a=require('./angular.json').projects; for (const p of ['shared','storefront','backoffice']) if(a[p].architect.test.options.coverageThresholds.lines!==80) process.exit(1)"
verificar "CA-0b-02 shared/api queda fuera de la cobertura" \
  node -e "const a=require('./angular.json').projects; if(!a.shared.architect.test.options.coverageExclude.some(x=>x.includes('lib/api'))) process.exit(1)"
verificar "CA-0b-02 npm run lint existe" node -e "if(!require('./package.json').scripts.lint) process.exit(1)"

echo "# CA-0b-03 el proxy de desarrollo envía /api al gateway (localhost:8080)"
verificar "CA-0b-03 proxy.conf.json: /api va a http://localhost:8080" \
  node -e "const p=require('./proxy.conf.json')['/api']; if(!p||p.target!=='http://localhost:8080') process.exit(1)"
verificar "CA-0b-03 ng serve de storefront (4200) y backoffice (4300) usa el proxy" \
  node -e "const a=require('./angular.json').projects; const o=(n)=>a[n].architect.serve.options; if(o('storefront').proxyConfig!=='proxy.conf.json'||o('backoffice').proxyConfig!=='proxy.conf.json'||o('storefront').port!==4200||o('backoffice').port!==4300) process.exit(1)"

echo "# CA-0b-04 un PR que solo toca frontend/ corre lint, pruebas y build del frontend, con caché de npm, y no el backend"
verificar "CA-0b-04 existe el job frontend" test -n "$trabajo_frontend"
verificar "CA-0b-04 el job frontend usa caché de npm" grep -q 'cache: npm' <<<"$trabajo_frontend"
verificar "CA-0b-04 el job frontend corre lint, pruebas con cobertura y build" \
  bash -c 'grep -q "npm run lint" <<<"$0" && grep -q "npm test -- --watch=false --coverage" <<<"$0" && grep -q "npm run build" <<<"$0"' "$trabajo_frontend"
verificar "CA-0b-04 el job frontend solo corre si cambia frontend/ o contracts/" \
  bash -c 'grep -q "needs.cambios.outputs.frontend" <<<"$0" && grep -Eq "\^\(frontend/\|contracts/" '"$ci" "$trabajo_frontend"
verificar "CA-0b-04 un cambio solo en frontend/ no compila el backend (el filtro del backend no incluye frontend/)" \
  bash -c 'regex="$(grep -oE "grep -qE .\^\(pom[^\x27]*" '"$ci"' | head -1 | sed "s/grep -qE .//")"; [[ -n "$regex" ]] && ! grep -qE "$regex" <<<"frontend/package.json" && ! grep -qE "$regex" <<<"frontend/projects/shared/src/public-api.ts"'

echo "# CA-0b-30 el job frontend verifica que los clientes de API no estén desactualizados"
verificar "CA-0b-30 el job frontend corre scripts/verificar-clientes.sh" grep -q 'scripts/verificar-clientes.sh' <<<"$trabajo_frontend"

[[ "$fallos" -eq 0 ]] || { echo "$fallos prueba(s) fallaron"; exit 1; }
echo "Todo en verde"
