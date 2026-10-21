package com.fitstore.payment.controller;

import com.fitstore.payment.service.DescargaBoleta;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Respuesta común de las descargas de boleta: el PDF sin envoltorio (K4) y como adjunto. */
final class BoletaRespuestas {

    private BoletaRespuestas() {
    }

    static ResponseEntity<byte[]> pdf(DescargaBoleta boleta) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(boleta.nombreArchivo()).build().toString())
                .body(boleta.contenido());
    }
}
