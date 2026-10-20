package com.fitstore.auth.service;

import com.fitstore.auth.dto.request.RegistroRequest;
import com.fitstore.auth.security.SesionEmitida;

public interface RegistroService {

    /**
     * Crea la cuenta del cliente con sus dos consentimientos y el evento {@code ClienteRegistrado}, en una transacción,
     * y deja la sesión iniciada.
     *
     * @param ip dirección desde la que acepta las políticas (prueba del consentimiento, Ley 29733)
     */
    SesionEmitida registrar(RegistroRequest solicitud, String ip);
}
