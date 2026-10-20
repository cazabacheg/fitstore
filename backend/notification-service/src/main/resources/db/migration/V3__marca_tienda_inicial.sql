-- Fila inicial de marca_tienda (SPEC-7 sección 6): la marca de demostración de FitStore, con la forma de
-- ConfiguracionPublica de settings. El primer ConfiguracionActualizada (version >= 1) la reemplaza.
INSERT INTO marca_tienda (id, configuracion, correos_alertas, version, actualizado_en)
VALUES (
    1,
    '{
      "identidad": {"nombre": "FitStore"},
      "tema": {
        "colorPrimario": "#C9B28C",
        "colorSecundario": "#2B2621",
        "colorAcento": "#4E5B45",
        "colorFondo": "#FAF9F7",
        "colorTexto": "#26221D"
      },
      "contenido": {
        "email": "ventas@tienda.pe",
        "telefono": "987654321",
        "horario": "Lunes a sábado de 9:00 a 19:00",
        "tiendaFisica": {"direccion": "Galería Gamarra, Jr. Gamarra 700, tienda 210"}
      }
    }'::jsonb,
    ARRAY['ventas@tienda.pe'],
    0,
    now()
);
