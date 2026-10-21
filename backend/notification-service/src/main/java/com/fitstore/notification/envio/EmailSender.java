package com.fitstore.notification.envio;

/** Puerto de salida del correo (micro D1): hoy un solo adaptador SMTP sirve a Mailpit y a la interfaz SMTP de SES. */
public interface EmailSender {

    /** @throws EnvioCorreoException si el servidor rechaza el correo o no responde */
    void enviar(MensajeCorreo mensaje);
}
