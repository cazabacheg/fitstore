package com.fitstore.settings.pruebas;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Devuelve la configuración y las tablas que tocan las pruebas que SÍ confirman en la base (sin la transacción de
 * la prueba) a los valores de la semilla de V3, para no contaminar a las demás pruebas.
 */
public final class RestauraSemilla {

    private RestauraSemilla() {
    }

    public static void ejecutar(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM imagen_tienda");
        jdbc.update("DELETE FROM outbox_evento");
        jdbc.update("""
                UPDATE configuracion_tienda SET
                    nombre = 'FitStore', eslogan = 'Ropa deportiva hecha en Gamarra.',
                    razon_social = 'Moda Activa S.A.C.', ruc = '20123456789',
                    direccion_fiscal = 'Jr. Gamarra 700, tienda 210, La Victoria, Lima',
                    color_primario = '#C9B28C', color_secundario = '#2B2621', color_acento = '#4E5B45',
                    color_fondo = '#FAF9F7', color_texto = '#26221D',
                    hero_titulo = 'Muévete con estilo',
                    hero_subtitulo = 'Leggings, tops y casacas hechos en Gamarra, con tela de compresión y secado rápido.',
                    hero_cta_texto = 'Ver novedades', hero_cta_enlace = '/productos', beneficios = '[]'::jsonb,
                    email = 'ventas@tienda.pe', telefono = '987654321', whatsapp = '987654321',
                    instagram_url = NULL, facebook_url = NULL, tiktok_url = NULL,
                    horario = 'Lunes a sábado de 9:00 a 19:00',
                    tienda_direccion = 'Galería Gamarra, Jr. Gamarra 700, tienda 210', tienda_ubigeo = '150115',
                    tienda_referencia = NULL,
                    metodos_pago = ARRAY['CULQI', 'YAPE', 'MERCADO_PAGO'],
                    modalidades_entrega = ARRAY['DELIVERY_LOCAL', 'AGENCIA', 'RECOJO'],
                    expiracion_pedido_minutos = 30, dias_reembolso_postventa = 30,
                    correos_alertas = ARRAY['ventas@tienda.pe'], version = 0, actualizado_por = NULL
                WHERE id = 1
                """);
    }
}
