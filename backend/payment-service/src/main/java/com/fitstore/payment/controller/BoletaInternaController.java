package com.fitstore.payment.controller;

import com.fitstore.payment.service.BoletaService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ruta interna para notification, que adjunta la boleta al correo de pedido confirmado. El gateway no la enruta (K3). */
@RestController
@RequestMapping("/internal/v1/boletas")
@Validated
public class BoletaInternaController {

    private final BoletaService boletas;

    public BoletaInternaController(BoletaService boletas) {
        this.boletas = boletas;
    }

    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Boleta simulada en PDF.",
            content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary")))
    @GetMapping(path = "/{numero}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> obtenerBoletaInterna(@PathVariable @Pattern(regexp = "^B001-[0-9]{8}$") String numero) {
        return BoletaRespuestas.pdf(boletas.descargarPorNumero(numero));
    }
}
