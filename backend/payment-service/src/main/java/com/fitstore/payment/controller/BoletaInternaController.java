package com.fitstore.payment.controller;

import com.fitstore.payment.service.BoletaService;
import jakarta.validation.constraints.Pattern;
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

    @GetMapping("/{numero}")
    public ResponseEntity<byte[]> obtenerBoletaInterna(@PathVariable @Pattern(regexp = "^B001-[0-9]{8}$") String numero) {
        return BoletaRespuestas.pdf(boletas.descargarPorNumero(numero));
    }
}
