package com.fitstore.notification.messaging.manejador;

import com.fitstore.notification.messaging.ManejadorEvento;
import com.fitstore.notification.messaging.evento.ConfiguracionActualizada;
import com.fitstore.notification.plantillas.MarcaService;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** No envía correo: actualiza la copia local de la marca y de los "correos para alertas" (D3 y D5). */
@Component
class ManejadorConfiguracionActualizada implements ManejadorEvento<ConfiguracionActualizada> {

    private final MarcaService marca;

    ManejadorConfiguracionActualizada(MarcaService marca) {
        this.marca = marca;
    }

    @Override
    public String tipo() {
        return "ConfiguracionActualizada";
    }

    @Override
    public Class<ConfiguracionActualizada> clase() {
        return ConfiguracionActualizada.class;
    }

    @Override
    public void manejar(UUID eventoId, ConfiguracionActualizada evento) {
        marca.actualizar(evento);
    }
}
