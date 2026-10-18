# 5. Portabilidad entre nubes y costo por cliente

Date: 2026-10-16

## Status

Accepted (optimización pospuesta)

## Decisión

- **Contexto:** FitStore se quiere comercializar; el costo de infraestructura por cliente debe ser bajo y no atarse a un proveedor.
- **Decisión (principio):** todo es **cloud-agnostic**: contenedores OCI, PostgreSQL estándar, SMTP estándar, almacenamiento de archivos detrás de una interfaz compatible con S3, configuración por variables de entorno vía config-server. Migrar de Azure a AWS (EC2 / Lightsail) es copiar el `docker-compose` y los datos.
- **Optimización de costo por cliente (pospuesta — no se aborda en esta versión):**
  - Imágenes multi-arquitectura (amd64 + arm64) para usar instancias ARM (Graviton en AWS, Ampere en Azure), más baratas.
  - **Imágenes nativas con GraalVM** (Spring Boot AOT): cada servicio baja de ~300 MB a ~60–100 MB de RAM y arranca en < 1 s → la instalación completa cabe en ~2 GB → instancia de 2–4 GB en lugar de 8 GB. A evaluar como mejora posterior (compilación lenta, ajustes de reflexión).
  - Perfil de despliegue "compacto" para clientes pequeños (límites de memoria más ajustados, Eureka/config con recursos mínimos).
- **Nota honesta:** para una MYPE, 10 microservicios por cliente son más caros que un monolito o un SaaS multi-tenant. La arquitectura se justifica por objetivos académicos y de escalabilidad; si FitStore se comercializa a escala, el siguiente paso natural sería multi-tenant (fuera de alcance).

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
