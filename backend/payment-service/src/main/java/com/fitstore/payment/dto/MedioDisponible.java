package com.fitstore.payment.dto;

import com.fitstore.payment.model.Medio;
import com.fitstore.payment.model.Proveedor;

/** Un medio que la página de pago puede ofrecer (P7): la página no decide nada por su cuenta. */
public record MedioDisponible(Proveedor proveedor, Medio medio, String clavePublica, boolean disponible,
        String motivoNoDisponible) {
}
