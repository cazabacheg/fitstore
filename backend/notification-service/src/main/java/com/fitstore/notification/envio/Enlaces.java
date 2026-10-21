package com.fitstore.notification.envio;

import com.fitstore.notification.config.PropiedadesNotificacion;
import org.springframework.stereotype.Component;

/** Enlaces de los botones: solo apuntan a la tienda propia (micro 8). */
@Component
public class Enlaces {

    private final PropiedadesNotificacion propiedades;

    public Enlaces(PropiedadesNotificacion propiedades) {
        this.propiedades = propiedades;
    }

    public String tienda() {
        return propiedades.enlaces().storefront();
    }

    public String pedido(String codigo) {
        return tienda() + "/cuenta/pedidos/" + codigo;
    }

    public String reclamos() {
        return tienda() + "/libro-de-reclamaciones";
    }

    /** Primer nombre para el saludo ("Ana Quispe" → "Ana"). */
    public static String primerNombre(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return "";
        }
        return nombreCompleto.trim().split("\\s+")[0];
    }
}
