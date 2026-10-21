package com.fitstore.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PropiedadesNotificacionTest {

    private static PropiedadesNotificacion con(String remitente) {
        return new PropiedadesNotificacion(remitente, null, null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("MAIL_FROM con nombre y dirección deja solo la dirección: el nombre del remitente sale de la marca")
    void remitenteConNombre() {
        assertThat(con("FitStore <no-responder@fitstore.local>").remitente()).isEqualTo("no-responder@fitstore.local");
    }

    @Test
    @DisplayName("MAIL_FROM con solo la dirección se usa tal cual; vacío usa la dirección por defecto")
    void remitenteSoloDireccion() {
        assertThat(con("ventas@tienda.pe").remitente()).isEqualTo("ventas@tienda.pe");
        assertThat(con(" ").remitente()).isEqualTo("no-responder@fitstore.local");
    }
}
