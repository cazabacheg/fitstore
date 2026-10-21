package com.fitstore.notification.envio;

/** Un correo listo para el SMTP: HTML y texto plano, y la boleta adjunta si corresponde. */
public record MensajeCorreo(String remitente, String nombreRemitente, String destinatario, String asunto, String html,
        String texto, AdjuntoCorreo adjunto) {

    /** Archivo adjunto (la boleta en PDF). */
    public record AdjuntoCorreo(String nombre, String tipo, byte[] contenido) {
    }
}
