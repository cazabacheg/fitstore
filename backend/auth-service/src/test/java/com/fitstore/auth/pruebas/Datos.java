package com.fitstore.auth.pruebas;

import com.fitstore.auth.model.Cliente;
import com.fitstore.auth.model.Direccion;
import com.fitstore.auth.model.RolStaff;
import com.fitstore.auth.model.Staff;
import java.time.Instant;

/** Fábricas de entidades válidas para las pruebas. */
public final class Datos {

    private Datos() {
    }

    public static Cliente cliente(String email, String dni) {
        Cliente c = new Cliente();
        c.setEmail(email);
        c.setPasswordHash("$2a$10$" + "x".repeat(53));
        c.setNombres("Lucía");
        c.setApellidos("Quispe Huamán");
        c.setDni(dni);
        c.setTelefono("987654321");
        c.setActivo(true);
        c.setCredencialesActualizadasEn(Instant.now());
        return c;
    }

    public static Staff staff(String email, RolStaff rol) {
        Staff s = new Staff();
        s.setEmail(email);
        s.setNombres("Marco");
        s.setApellidos("Rojas");
        s.setRol(rol);
        s.setActivo(true);
        s.setCredencialesActualizadasEn(Instant.now());
        return s;
    }

    public static Direccion direccion(Cliente cliente, String alias, boolean predeterminada) {
        Direccion d = new Direccion();
        d.setCliente(cliente);
        d.setAlias(alias);
        d.setUbigeo("150101");
        d.setDireccion("Jr. Gamarra 123, La Victoria");
        d.setPredeterminada(predeterminada);
        return d;
    }
}
