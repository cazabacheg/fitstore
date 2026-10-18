# SPEC-XX — <Título>

<!-- Plantilla v2 (2026-10-18): los CA van dentro de cada HU; las HU citan sus pantallas P-XX-NN. -->

> Versión: **v1 (borrador)** — AAAA-MM-DD · Estados: ✅ decidido · 🟡 propuesto · ❓ abierto
> **Dueña de:** <servicio(s) backend> · <área(s) de pantallas>
> **Depende de:** <specs que deben estar cerradas antes> · **Habilita:** <specs que la necesitan>
> **Contratos:** `contracts/openapi/<servicio>.yaml`, `contracts/asyncapi/fitstore-eventos.yaml` (etapa de contratos)

**Historial:**
- v1 (AAAA-MM-DD): borrador inicial.

**Estado de revisión por sección:** 1 ⏳ · 2 ⏳ · 3 ⏳ · 4 ⏳ · 5 ⏳ · 6 ⏳ · 7 ⏳ · 8 ⏳ · 9 ⏳

---

## 1. Objetivo y alcance

Qué entrega esta spec al cerrarse (algo que funcione de punta a punta).

**Incluye:** …
**No incluye (y dónde va):** …

## 2. Referencias

Micros, ADRs, RF/RNF y CU que implementa. **No se repite** lo que ya está en los micros: se enlaza la sección.

## 3. Historias de usuario y criterios de aceptación

Cada historia lleva **debajo sus criterios de aceptación** y la referencia a sus pantallas (sección 4).

### HU-XX-01 — <título corto>

Como <actor>, quiero <acción>, para <beneficio>. · Cubre: CU-…, RF-… · Pantallas: [P-XX-01](#p-xx-01)

```gherkin
CA-XX-01  Dado …
          Cuando …
          Entonces …

CA-XX-02  Dado …
          Cuando …
          Entonces …
```

Cada criterio se convierte en al menos una prueba (sección 7).

## 4. Pantallas

Una subsección por pantalla, con id `P-XX-NN` (el que citan las historias):

### P-XX-01 — <nombre>

- **Ruta / proyecto:** `/…` · storefront o backoffice · rol requerido
- **Boceto:** `![P-XX-01](img/P-XX-01.png)` (imagen en `docs/specs/img/`)
- **Componentes:** propios de la funcionalidad y de `shared`
- **Estados:** cargando · vacío · error · éxito
- **Validaciones:** …
- **Endpoints que usa:** …

"No aplica" si la spec no tiene frontend.

## 5. Backend

Endpoints, eventos y reglas **por referencia** al micro (sus secciones 4, 6 y 7). Aquí solo van las decisiones de implementación que el micro no cubre.

## 6. Datos

Migraciones (después de `V1`, con versión fecha-hora `VAAAAMMDD_HHMM__descripcion.sql`), datos semilla, datos de prueba.

## 7. Pruebas

Tabla `CA → prueba` (tipo: datos, unitaria, API, integración, E2E, Postman, k6) y ubicación.

## 8. Archivos y carpetas que toca

| Ruta | Acción | Nota |
|---|---|---|
| `…` | **crea** / **modifica** / **solo lectura** | |

Reglas comunes (ver `README.md`): `fitstore-common`, `projects/shared`, `api-gateway` y `infra/config-repo/application.yml` están **congelados** fuera de su spec dueña; un cambio ahí se pide y se hace en un PR aparte.

## 9. Definición de terminado

- [ ] Todos los `CA` pasan en CI.
- [ ] Postman de la spec actualizado.
- [ ] OpenAPI del servicio coincide con lo implementado.
- [ ] Sin secretos en el repo; sin atribución de IA en commits ni README.
- [ ] Revisado y aprobado por el autor.

## Preguntas abiertas ❓
