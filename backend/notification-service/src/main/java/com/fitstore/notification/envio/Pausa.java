package com.fitstore.notification.envio;

/** Espera entre un correo y el siguiente para respetar el ritmo máximo; las pruebas la reemplazan para no esperar. */
@FunctionalInterface
public interface Pausa {

    Pausa DE_VERDAD = milisegundos -> {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    };

    void esperar(long milisegundos);
}
