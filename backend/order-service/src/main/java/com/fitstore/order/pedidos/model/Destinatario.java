package com.fitstore.order.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Quién recibe el pedido: copia congelada de los datos del cliente al confirmar (D3). */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Destinatario {

    @Column(name = "dest_nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "dest_apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "dest_dni", nullable = false, length = 8)
    private String dni;

    @Column(name = "dest_telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "dest_email", nullable = false, length = 254)
    private String email;
}
