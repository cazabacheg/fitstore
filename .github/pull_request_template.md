## Qué cambia

<!-- Spec o paso (ej.: SPEC-0a 0a.3), rama y resumen en 2 o 3 líneas. -->

## Lista de verificación (estrategia multiagente, V1 a V11)

Marca cada punto con su evidencia (salida del comando o enlace). El PR se integra solo con los checks en verde.

- [ ] **V1 Criterios con prueba:** `scripts/ca-cobertura.sh <spec>` sin faltantes.
- [ ] **V2 Pruebas en verde:** `./mvnw -pl backend/<módulo> -am verify` (frontend: `npm run lint` y `npm test`).
- [ ] **V3 Las pruebas prueban algo:** el commit en rojo existe antes del código; no hay pruebas borradas, deshabilitadas ni debilitadas.
- [ ] **V4 Solo lo mío:** `scripts/fuera-de-alcance.sh <brief>.alcance` sin archivos fuera de alcance.
- [ ] **V5 Contratos respetados:** `scripts/contrato.sh <servicio>` sin cambios incompatibles; eventos iguales al AsyncAPI.
- [ ] **V6 Diseño respetado** (si hay pantallas): capturas a 1280 y 390 px contra `docs/diseno/`.
- [ ] **V7 Higiene:** gitleaks limpio, sin secretos, commits sin atribución de IA y con autor `cazabacheg`, migraciones con el nombre convenido.
- [ ] **V8 CI en verde:** `ci.yml` (y `stage.yml` si va a `stage` o `main`).
- [ ] **V9 Definición de terminado:** cada casilla de la sección 9 de la spec que no sea del autor.
- [ ] **V10 Cobertura:** al menos 80 % de líneas por módulo y el núcleo cubierto (informe de JaCoCo o Vitest).
- [ ] **V11 Alcance exacto:** todo lo de la spec está hecho y no sobra nada que la spec no pida.

## Tabla CA a prueba

| CA | Archivo de prueba o verificación |
|---|---|
| | |

## Pendientes o dudas
