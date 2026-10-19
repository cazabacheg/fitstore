# 13. Almacenamiento de archivos con puerto y adaptadores (S3 y Azure Blob)

Date: 2026-10-16

## Status

Accepted

## Decisión

- **Contexto:** se guardan imágenes de productos, logos y boletas PDF. v1 proponía MinIO, pero **MinIO dejó de publicar imágenes Docker de su edición comunitaria en octubre de 2025 y la pasó a modo mantenimiento** → ya no es una base sana para un producto.
- **Decisión:**
  - Interfaz (puerto) `FileStorage` en el módulo `common` con dos adaptadores intercambiables por configuración:
    - `S3FileStorage` (AWS SDK v2): funciona con **AWS S3** y con cualquier servidor compatible con S3.
    - `AzureBlobFileStorage` (Azure SDK): para **Azure Blob Storage**, que no habla el protocolo S3.
  - En local y en la VM: **Garage** (servidor S3 open source, liviano, imágenes oficiales; ~64 MB de RAM). SeaweedFS queda como alternativa.
- **¿Por qué no AWS S3 directamente?** No es gratis para esta cuenta: el saldo gratuito de S3 (5 GB) era de 12 meses desde la creación de la cuenta, y la cuenta del autor es antigua. El costo sería de centavos (~US$0.023 por GB al mes + solicitudes), pero suma una segunda nube, credenciales y latencia entre Azure y AWS. Garage en la VM cuesta US$0 (usa el disco que ya existe). Pasar a S3 al vender en AWS es solo configuración.
- **Portabilidad:** en **AWS** se apunta el adaptador S3 a S3 real; en **Azure** se puede (a) seguir con Garage en la VM (cero cambios) o (b) activar el adaptador Blob para usar el almacenamiento administrado de Azure. Ambos casos son solo configuración, así que la migración es igual de fácil en las dos nubes.
- **Alternativas:** disco de la VM (no portable), Cloudinary (externo; transforma imágenes), compilar MinIO desde el código fuente (mantenimiento propio).
- **Actualización (2026-10-19, SPEC-8 I6):** la demo en Azure usa la opción (a), **Garage en la VM**. El adaptador `AzureBlobFileStorage` queda **diferido** (no se implementa en el MVP); el puerto `FileStorage` permite agregarlo sin tocar los servicios.

Fuente: `docs/analisis/01-analisis-macro.md`, sección 6.
