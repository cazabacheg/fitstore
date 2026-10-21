package com.fitstore.notification.envio;

/** El correo no salió (SMTP caído o rechazo); el despachador lo reintenta. */
public class EnvioCorreoException extends RuntimeException {

    public EnvioCorreoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
