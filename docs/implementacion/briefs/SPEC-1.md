# Brief — SPEC-1 Identidad

> Preparado por el lead · 2026-10-20 · Spec: [SPEC-1](../../specs/SPEC-1-identidad.md) **v6** · Modelo del implementador: Sonnet · Ola 2a (en paralelo con las SPEC-2 y SPEC-3)
> Reglas comunes: [`CLAUDE.md`](../../../CLAUDE.md) · Estrategia: [estrategia multiagente](../estrategia-multiagente.md)

Un agente, **backend y luego frontend** (O3). Rama `spec/1-identidad`, alcance [`SPEC-1.alcance`](SPEC-1.alcance).

## Validación del brief (estrategia B2)

Copiada de la sección 7 de la SPEC-1 v6. Un solo paso: todos los criterios son de este agente.

| CA | Sección 7: tipo · dónde | Dueño | Nota del lead |
|---|---|---|---|
| 01–03 | API (MockMvc) + componente · `RegistroControllerTest`, `formulario-registro.spec.ts` | SPEC-1 | El componente va en `storefront/.../features/acceso/` |
| 04–07, 22–24 | API + unitaria · `AutenticacionServiceTest` (bloqueo con Redis en Testcontainers), `AuthControllerTest` | SPEC-1 | 04 y 22 también tienen pruebas de componente (vuelta a `returnUrl`, dashboard con el menú del rol) |
| 08–11 | Unitaria + integración · `TokenServiceTest` | SPEC-1 | — |
| 12–14 | Unitaria + API · `PasswordServiceTest`, evento en el outbox | SPEC-1 | — |
| 15–16, 35 | API · `ClienteControllerTest`, `StaffControllerTest` | SPEC-1 | Más las pruebas de componente de P-1-05 y P-1-10 |
| 17–21 | API + datos · `DireccionServiceTest`, propiedad (404 ajeno) | SPEC-1 | Más el componente de P-1-06 (botón deshabilitado con 10, CA-1-20) |
| 25–31 | API · `StaffServiceTest` | SPEC-1 | — |
| 32–34 | API · `ClienteAdminControllerTest` | SPEC-1 | Más el componente de P-1-09 (OPERADOR sin botón "Bloquear", CA-1-34) |
| 36–37 | Integración · gateway + un servicio con un token real de auth | SPEC-1 | El gateway está congelado. **36:** prueba de integración en auth (un token emitido por auth se valida con un `JwtDecoder` que lee `/.well-known/jwks.json` del propio servicio levantado, y se respetan `rol` y `tipo`) y la colección Postman a través del gateway (token real → 200; token adulterado → 401). **37:** `RetiroJwksPruebaTest` en auth lee el repositorio: `infra/dev/jwks-prueba.json` y su clave no existen, y nada en `infra/` usa `jwks-archivo` ni `FITSTORE_SECURITY_JWKS_ARCHIVO` |
| 38 | Unitaria · `AdminInicialTest` | SPEC-1 | — |
| 39 | Unitaria · `DatosDemoNubeTest` | SPEC-1 | — |
| Todas | Postman · `postman/auth.postman_collection.json` | SPEC-1 | Flujo cliente y flujo staff; cada petición con el id de su criterio en el nombre |

**Incluye (sección 1) y sección 8:** todo es de este agente: la lógica de auth, los 3 eventos, P-1-01 a P-1-10, el retiro del JWKS de prueba, los datos demo y Postman. Las imágenes `docs/specs/img/P-1-*` ya existen. **No incluye:** correos (spec 7), fusión del carrito (spec 4), historial de pedidos del cliente (spec 4), verificación de correo y eliminación de cuenta.

`scripts/ca-cobertura.sh 1` debe dar **39 de 39**.

## 1. Lectura obligatoria (en este orden)

1. [`CLAUDE.md`](../../../CLAUDE.md).
2. [SPEC-1](../../specs/SPEC-1-identidad.md) v6 completa. **La spec manda.**
3. Micro [`auth-service.md`](../../analisis/micro/auth-service.md) completo: modelo D1–D6, invariantes, endpoints 4.1, flujos 4.2–4.7, eventos, seguridad y la sección 10, "Diseño técnico", con los paquetes `controller`, `dto`, `mapper`, `service`, `security`, `exception`.
4. Contratos: [`contracts/openapi/auth-service.yaml`](../../../contracts/openapi/auth-service.yaml), `comun.yaml` y los 3 canales de auth en [`fitstore-eventos.yaml`](../../../contracts/asyncapi/fitstore-eventos.yaml).
5. La SPEC-0b, secciones R2, R6, R7 y R11, y el código de `frontend/projects/shared/src/public-api.ts`: `SesionStore` (`iniciarSesion`, `cerrarSesion`), `authApi`, `camposConError`, `SIN_TOAST`, guards.
6. El código de 0a de auth (`backend/auth-service`): entidades, repositorios, `V1`, `CatalogoRutasEventosAuth` y sus pruebas, que son ejemplos de cómo se prueba aquí.
7. [`docs/demo/ids-demo.md`](../../demo/ids-demo.md) (UUID fijos de las cuentas demo).

En tu primer mensaje: qué leíste, en 5–10 líneas qué construyes, y cómo probarás CA-1-36 y CA-1-37.

## 2. Qué construyes

**Backend (`backend/auth-service`), por historia y con TDD:**
1. Registro, los dos logins, bloqueo por intentos con Redis (independiente para cliente y staff), emisión de JWT RS256 con `sub`, `tipo` y `rol`, refresh con rotación y detección de reutilización, logout, invalidación por `credenciales_actualizadas_en`, y la cookie de A4.
2. Recuperación e invitación (token SHA-256, 30 min o 24 h, el anterior queda inválido) y los eventos por el outbox de `fitstore-common`.
3. Perfil, direcciones (A1, A2, máximo 10), staff (invariante del último ADMIN, sin auto-desactivarse, reenvío), consulta y bloqueo de clientes.
4. `AdminInicial` (CA-1-38), JWKS público y `DatosDemoNube` (perfil `demo`, CA-1-39).
5. `R__datos_demo.sql` en `db/dev/` con los UUID de `ids-demo.md`: 4 clientes (direcciones en Lima, Callao y Arequipa) y 1 OPERADOR.
   - La contraseña común de desarrollo va documentada en el `README` del servicio (`backend/auth-service/README.md`).
   - En el informe final reporta los nombres, correos y DNI que elegiste; el lead los anota en `ids-demo.md`.
6. **Retiro del JWKS de prueba (A5):**
   - borra `infra/dev/jwks-prueba.json` y `infra/dev/clave-privada-PRUEBA-solo-dev.jwk.json`;
   - en `infra/docker-compose.dev.yml`, quita la variable `FITSTORE_SECURITY_JWKS_ARCHIVO` y el volumen;
   - en `infra/config-repo/application.yml`, agrega `spring.security.oauth2.resourceserver.jwt.jwk-set-uri: http://auth-service:8081/.well-known/jwks.json` para todos los servicios.
   - **No toques `.gitleaks.toml` ni `.gitleaksignore`:** la clave sigue en el historial de Git y gitleaks lo revisa completo.
7. `ContratoOpenApiTest` en verde contra `auth-service.yaml`. El lead agrega `auth-service` a `contracts/implementados.txt` al verificar (V5).

**Frontend:**
- **Tienda:** `features/acceso` (P-1-01 a P-1-04) y `features/cuenta` (P-1-05 y P-1-06, con `CuentaLayout` y `DialogoDireccion` reutilizable según A6).
- **Backoffice:**
  - `features/acceso` (P-1-07 fuera del *shell*, y P-1-03 y P-1-04 con su endpoint de staff);
  - `features/perfil` (P-1-10);
  - `features/usuarios` (P-1-08 Staff y P-1-09 Clientes, con `FsTabla` y R11).
- **Login:**
  - vuelve a `returnUrl` (CA-1-04, el parámetro que pone el guard de la SPEC-0b);
  - si ya hay sesión, `/ingresar` de la tienda lleva a `/cuenta/perfil`;
  - al ingresar llama a `SesionStore.iniciarSesion`;
  - el logout llama a `/auth/logout` y luego a `cerrarSesion`.

**Postman:** `postman/auth.postman_collection.json`, flujo cliente y flujo staff. Usa el entorno `postman/local.postman_environment.json`; si necesitas una variable nueva, pídemela.

## 3. Reglas propias

- **Rutas:** conserva los nombres que el *shell* importa: `ACCESO_ROUTES` y `CUENTA_ROUTES` (tienda); `ACCESO_ROUTES`, `PERFIL_ROUTES`, `STAFF_ROUTES` y `CLIENTES_ROUTES` (backoffice). Dentro de cada arreglo declaras tus subrutas (`/clientes/:id`). Las guardas de rol ya están en el *shell*.
- **Congelados:** `shared`, `ui/`, `layout/`, `app.routes.ts`, gateway y `fitstore-common`. Las únicas excepciones son los dos archivos de infra de A5.
- **Seguridad:** sin contraseñas reales en el repo. La contraseña de desarrollo es de demostración y se documenta como tal. `FITSTORE_ADMIN_*` y `DEMO_PASSWORD` salen del entorno; si falta alguna en `.env.example`, pídemela, porque ese archivo es del lead.
- **Mientras trabajas en paralelo**, las specs 2 y 3 siguen usando el JWKS de prueba en sus ramas. No afecta a sus pruebas (usan su propio `jwk-set-uri` de prueba); el cambio de A5 llega a todos cuando se integre tu PR.

**Núcleo (cobertura completa):** `TokenService`, autenticación y bloqueo, `PasswordService`, `DireccionService`, `StaffService` (invariantes), `AdminInicial`, `DatosDemoNube`, la publicación de eventos y, en el frontend, el ingreso con su vuelta a `returnUrl` y el `DialogoDireccion`.
