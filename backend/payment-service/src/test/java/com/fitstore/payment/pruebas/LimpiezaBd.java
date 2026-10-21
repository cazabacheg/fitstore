package com.fitstore.payment.pruebas;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Las pruebas de API e integración confirman datos de verdad (el cobro no corre en la transacción de la prueba) y todas
 * comparten un PostgreSQL con las pruebas de repositorio. Esto deja la base como la dejó la migración.
 */
public final class LimpiezaBd {

    private LimpiezaBd() {
    }

    public static void vaciar(JdbcTemplate jdbc) {
        jdbc.execute("TRUNCATE reembolso, boleta, transaccion, webhook_recibido, intencion_pago, outbox_evento, evento_procesado");
        jdbc.update("UPDATE correlativo_boleta SET ultimo = 0");
    }
}
