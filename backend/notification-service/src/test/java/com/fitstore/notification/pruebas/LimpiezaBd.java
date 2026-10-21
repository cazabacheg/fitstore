package com.fitstore.notification.pruebas;

import org.springframework.jdbc.core.JdbcTemplate;

/** Deja la base como la dejan las migraciones: las pruebas comparten un solo PostgreSQL. */
public final class LimpiezaBd {

    private static final String MARCA_INICIAL = """
            {"identidad": {"nombre": "FitStore"},
             "tema": {"colorPrimario": "#C9B28C", "colorSecundario": "#2B2621", "colorAcento": "#4E5B45",
                      "colorFondo": "#FAF9F7", "colorTexto": "#26221D"},
             "contenido": {"email": "ventas@tienda.pe", "telefono": "987654321",
                           "horario": "Lunes a sábado de 9:00 a 19:00",
                           "tiendaFisica": {"direccion": "Galería Gamarra, Jr. Gamarra 700, tienda 210"}}}
            """;

    private LimpiezaBd() {
    }

    public static void limpiar(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM envio_correo");
        jdbc.update("DELETE FROM pedido_resumen");
        jdbc.update("DELETE FROM evento_procesado");
        jdbc.update("UPDATE plantilla SET activa = true");
        jdbc.update("INSERT INTO marca_tienda (id, configuracion, correos_alertas, version, actualizado_en) "
                + "VALUES (1, ?::jsonb, ARRAY['ventas@tienda.pe'], 0, now()) "
                + "ON CONFLICT (id) DO UPDATE SET configuracion = EXCLUDED.configuracion, "
                + "correos_alertas = EXCLUDED.correos_alertas, version = 0", MARCA_INICIAL);
    }
}
