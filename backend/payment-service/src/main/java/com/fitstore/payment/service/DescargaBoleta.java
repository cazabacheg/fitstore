package com.fitstore.payment.service;

/** El PDF de una boleta listo para entregar; el archivo nunca tiene una URL pública (D6). */
public record DescargaBoleta(String numero, byte[] contenido) {

    /** Nombre del archivo que ve la persona: {@code Boleta-B001-00000046.pdf}. */
    public String nombreArchivo() {
        return "Boleta-" + numero + ".pdf";
    }
}
