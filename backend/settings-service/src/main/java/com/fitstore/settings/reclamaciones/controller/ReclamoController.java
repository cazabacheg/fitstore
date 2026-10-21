package com.fitstore.settings.reclamaciones.controller;

import com.fitstore.common.api.ApiResponse;
import com.fitstore.settings.reclamaciones.dto.ReclamoRegistradoDto;
import com.fitstore.settings.reclamaciones.dto.ReclamoRequest;
import com.fitstore.settings.reclamaciones.service.ReclamoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registro público del Libro de Reclamaciones (con o sin cuenta). El gateway limita 3 envíos por minuto por IP. */
@RestController
@RequestMapping(value = "/api/v1/reclamos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "reclamos")
public class ReclamoController {

    private final ReclamoService reclamos;

    public ReclamoController(ReclamoService reclamos) {
        this.reclamos = reclamos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "registrarReclamo", summary = "Registrar un reclamo o una queja")
    public ApiResponse<ReclamoRegistradoDto> registrar(@Valid @RequestBody ReclamoRequest solicitud,
            HttpServletRequest peticion) {
        return ApiResponse.ok("Registramos tu reclamo", reclamos.registrar(solicitud, ipDe(peticion)));
    }

    /** El gateway reenvía la IP original en {@code X-Forwarded-For}: la primera es la del consumidor. */
    private static String ipDe(HttpServletRequest peticion) {
        String reenviada = peticion.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return peticion.getRemoteAddr();
    }
}
