# auth-service

Identidad y acceso de FitStore: registro e ingreso de clientes y de staff, tokens JWT RS256, recuperación de contraseña,
perfil, direcciones, administración del personal y consulta de clientes. Puerto **8081**, base `auth_db` (PostgreSQL) y
Redis (prefijo `auth:`). Diseño completo: [`docs/analisis/micro/auth-service.md`](../../docs/analisis/micro/auth-service.md).
Spec: [SPEC-1](../../docs/specs/SPEC-1-identidad.md). Contrato: [`contracts/openapi/auth-service.yaml`](../../contracts/openapi/auth-service.yaml).

## Qué expone

| Grupo | Rutas | Quién |
|---|---|---|
| Sesión | `POST /api/v1/auth/{registro,login,staff/login,refresh,logout}` | público (refresh y logout usan la cookie `fs_refresh`) |
| Contraseña | `POST /api/v1/auth/password/{olvido,restablecer}`, `POST /api/v1/auth/staff/password/olvido` | público |
| Cliente | `/api/v1/clientes/me`, `/me/password`, `/me/direcciones/**` | CLIENTE |
| Staff propio | `/api/v1/staff/me`, `/me/password` | OPERADOR, ADMIN |
| Personal | `/api/v1/staff/**` | ADMIN |
| Clientes (backoffice) | `GET /api/v1/clientes`, `/{id}` · `PATCH /{id}/desactivar\|activar` | OPERADOR, ADMIN (bloquear: ADMIN) |
| JWKS | `GET /.well-known/jwks.json` | público |

Publica `ClienteRegistrado`, `PasswordResetSolicitado` y `PasswordCambiado` por el outbox (`auth.cliente.registrado`,
`auth.password.reset-solicitado`, `auth.password.cambiado`). Los correos los envía `notification-service`.

## Cómo se firman y validan los tokens

* Access token de 15 min (en memoria del navegador) y refresh de 7 días en la cookie `fs_refresh`
  (`HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth`, `Secure` salvo en dev). El refresh rota en cada uso; reutilizar uno
  ya usado responde 401 y queda en el registro como posible robo.
* El par RSA sale de los PEM de `JWT_PRIVATE_KEY_PATH` y `JWT_PUBLIC_KEY_PATH`. En el perfil `dev`, si faltan, se genera
  uno al arrancar (los tokens no sobreviven al reinicio). En cualquier otro perfil el servicio **no arranca** sin ellos.
* El gateway y los demás servicios validan con `GET /.well-known/jwks.json` (`jwk-set-uri` en
  `infra/config-repo/application.yml`); el JWKS de prueba de la 0a ya no existe.
* Al cambiar o restablecer la contraseña, o al desactivar una cuenta, se actualiza `credenciales_actualizadas_en`: todo
  refresh emitido antes se rechaza. En el refresh token esa marca viaja en el claim `cred` (milisegundos) para que sea
  exacta aunque el `iat` del JWT solo tenga segundos.

## Variables de entorno

| Variable | Uso |
|---|---|
| `AUTH_DB_USER`, `AUTH_DB_PASSWORD`, `DB_HOST` | Base `auth_db` |
| `JWT_PRIVATE_KEY_PATH`, `JWT_PUBLIC_KEY_PATH`, `JWT_KID` | Par RSA (PEM) y su `kid` |
| `FITSTORE_PUBLIC_URL`, `STOREFRONT_URL`, `BACKOFFICE_URL` | Emisor del JWT y enlaces de los correos |
| `FITSTORE_ADMIN_EMAIL`, `FITSTORE_ADMIN_PASSWORD` | Primer ADMIN: se crea al arrancar solo si no existe ningún ADMIN |
| `DEMO_PASSWORD` | Solo con el perfil `demo` (nube): contraseña de las cuentas de demostración; si falta, se desactivan |

## Datos de demostración (perfiles `dev` y `demo`)

La migración repetible `src/main/resources/db/dev/R__datos_demo.sql` carga, solo con esos perfiles, 4 clientes con los UUID
de [`docs/demo/ids-demo.md`](../../docs/demo/ids-demo.md) y 1 OPERADOR. El ADMIN no se siembra: lo crea `AdminInicial`.

**Contraseña común de desarrollo de todas las cuentas de demostración: `FitStoreDemo2026`.** Es de demostración y pública,
no es un secreto. En la nube (perfil `demo`) `DatosDemoNube` la reemplaza por `DEMO_PASSWORD` o desactiva las cuentas, así
que esa contraseña nunca sirve fuera de desarrollo.

| Cuenta | Nombre | Correo | DNI | Celular | Direcciones |
|---|---|---|---|---|---|
| Cliente demo 1 | Lucía Mendoza Quispe | lucia.mendoza@fitstore.demo | 70123401 | 987000001 | Miraflores (Lima) |
| Cliente demo 2 | Carlos Huamán Torres | carlos.huaman@fitstore.demo | 70123402 | 987000002 | Cercado de Lima y Callao |
| Cliente demo 3 | Rosa Ccori Vargas | rosa.ccori@fitstore.demo | 70123403 | 987000003 | Callao |
| Cliente demo 4 | Miguel Paredes Soto | miguel.paredes@fitstore.demo | 70123404 | 987000004 | Arequipa |
| Operador demo | Diana Flores Rivas | diana.flores@fitstore.demo | — | — | — |

## Pruebas

```bash
./mvnw -pl backend/auth-service -am verify     # pruebas + JaCoCo (mínimo 80 %); usa Docker (PostgreSQL y Redis)
scripts/contrato.sh auth-service               # compara el OpenAPI generado con el contrato
```

Las pruebas de API arrancan el servicio completo con PostgreSQL (`postgres:17.10`) y Redis (`redis:8.10.1`) en Testcontainers.
La colección Postman está en `postman/auth.postman_collection.json` (necesita la plataforma levantada).
