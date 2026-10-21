package com.fitstore.notification.envio;

import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Envía por SMTP (Mailpit en dev, SES en la nube) con el HTML, el texto plano alternativo y la boleta adjunta. */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender correo;

    public SmtpEmailSender(JavaMailSender correo) {
        this.correo = correo;
    }

    @Override
    public void enviar(MensajeCorreo mensaje) {
        try {
            MimeMessage mime = correo.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mime, true, "UTF-8");
            ayudante.setFrom(mensaje.remitente(), mensaje.nombreRemitente());
            ayudante.setTo(mensaje.destinatario());
            ayudante.setSubject(mensaje.asunto());
            ayudante.setText(mensaje.texto(), mensaje.html());
            if (mensaje.adjunto() != null) {
                ayudante.addAttachment(mensaje.adjunto().nombre(), new ByteArrayResource(mensaje.adjunto().contenido()),
                        mensaje.adjunto().tipo());
            }
            correo.send(mime);
        } catch (MailException | jakarta.mail.MessagingException | UnsupportedEncodingException e) {
            throw new EnvioCorreoException("No se pudo enviar el correo a " + mensaje.destinatario() + ": "
                    + e.getMessage(), e);
        }
    }
}
