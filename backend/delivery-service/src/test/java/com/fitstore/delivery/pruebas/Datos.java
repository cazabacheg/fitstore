package com.fitstore.delivery.pruebas;

import com.fitstore.delivery.model.Agencia;
import com.fitstore.delivery.model.Destinatario;
import com.fitstore.delivery.model.DireccionEnvio;
import com.fitstore.delivery.model.Entrega;
import com.fitstore.delivery.model.EstadoEntrega;
import com.fitstore.delivery.model.Modalidad;
import com.fitstore.delivery.model.Repartidor;
import com.fitstore.delivery.model.TarifaEnvio;
import com.fitstore.delivery.model.TipoVehiculo;
import java.math.BigDecimal;
import java.util.UUID;

/** Fábricas de entidades válidas para las pruebas de repositorio. */
public final class Datos {

    private Datos() {
    }

    public static TarifaEnvio tarifa(Modalidad modalidad, String ubigeo, String costo) {
        return TarifaEnvio.builder().modalidad(modalidad).ubigeo(ubigeo).costo(new BigDecimal(costo))
                .plazoDiasMin(1).plazoDiasMax(2).build();
    }

    public static Agencia agencia(String nombre, int orden) {
        return Agencia.builder().nombre(nombre).urlSeguimiento("https://rastreo.ejemplo.pe/x?codigo={codigo}")
                .orden(orden).build();
    }

    public static Repartidor repartidor(String nombres) {
        return Repartidor.builder().nombres(nombres).telefono("987654321").vehiculo(TipoVehiculo.MOTO)
                .placa("ABC-123").build();
    }

    public static Entrega entrega(Modalidad modalidad) {
        boolean local = modalidad == Modalidad.DELIVERY_LOCAL;
        return Entrega.builder()
                .pedidoId(UUID.randomUUID())
                .codigoPedido("FS-2026-000001")
                .modalidad(modalidad)
                .estado(local ? EstadoEntrega.PENDIENTE_ASIGNACION : EstadoEntrega.PENDIENTE_ENVIO)
                .destinatario(Destinatario.builder().nombres("Ana").apellidos("Quispe").dni("12345678")
                        .telefono("999888777").email("ana@correo.pe").build())
                .direccion(DireccionEnvio.builder().ubigeo("150115").departamento("Lima").provincia("Lima")
                        .distrito("La Victoria").direccion("Jr. Gamarra 123").referencia("Frente al banco").build())
                .resumenItems("2 x Polo deportivo M")
                .bultos(1)
                .build();
    }
}
