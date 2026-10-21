package com.fitstore.notification.plantillas;

/** Un correo ya armado: asunto, HTML con la marca y versión en texto plano. */
public record CorreoRenderizado(String asunto, String html, String texto) {
}
