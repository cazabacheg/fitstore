# Datos de entrada de `ubigeo-a-sql`

`ubigeo_distrito.csv` es la fuente de `V2__ubigeo_inei.sql` de settings-service.

- **Origen:** repositorio `jmcastagnetto/ubigeo-peru-aumentado` (datos derivados del INEI), fijado al commit
  `a783fa66effb16fa8f3321b49a3d5e71cab15a5c`.
- **Archivo:** `https://raw.githubusercontent.com/jmcastagnetto/ubigeo-peru-aumentado/a783fa66effb16fa8f3321b49a3d5e71cab15a5c/ubigeo_distrito.csv`
  (copia sin cambios).
- **Licencia:** MIT, del repositorio de origen.
- **Códigos:** se usa la columna `inei` (codificación del INEI de 6 dígitos: departamento, provincia y distrito).
  La columna `reniec` no se usa. Los nombres vienen en mayúsculas y sin tildes y se usan tal cual. Las demás
  columnas se ignoran.
- **Conteos:** 25 departamentos, 196 provincias y 1 893 distritos (el archivo tiene 1 895 líneas porque dos nombres de capital ocupan varias líneas). Los departamentos y las provincias se derivan
  de los códigos y nombres de los distritos.

- **Corrección:** a la fila de MOQUEGUA / MARISCAL NIETO / SAN ANTONIO la fuente le deja `inei` = `NA` (solo trae
  su código RENIEC 170107). `correcciones.csv` le asigna el código INEI `180107` (el distrito siguiente a los seis
  de la provincia, 180101 a 180106). El script solo aplica una corrección a una fila sin código válido.

Regenerar la migración:

```
infra/scripts/ubigeo-a-sql infra/scripts/ubigeo-a-sql-datos/ubigeo_distrito.csv \
  --correcciones infra/scripts/ubigeo-a-sql-datos/correcciones.csv \
  --salida backend/settings-service/src/main/resources/db/migration/V2__ubigeo_inei.sql
```
