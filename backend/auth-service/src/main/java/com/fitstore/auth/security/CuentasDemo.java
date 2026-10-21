package com.fitstore.auth.security;

import java.util.List;
import java.util.UUID;

/**
 * Cuentas de demostración con UUID fijos (docs/demo/ids-demo.md): los dueños de los pedidos de demostración de las
 * otras specs. Las carga {@code R__datos_demo.sql} en los perfiles dev y demo; en la nube las gestiona {@link DatosDemoNube}.
 */
public final class CuentasDemo {

    public static final List<UUID> CLIENTES = List.of(
            UUID.fromString("35a11d4f-054a-44a5-8ac4-cf0ed4020f8c"),
            UUID.fromString("41f4b1c9-00e0-4816-91b9-a1a143a69df9"),
            UUID.fromString("a3d50456-6d33-4e6e-80fc-62563fc46808"),
            UUID.fromString("652dcca9-9c40-434a-9d2d-33e44d1dcc16"));

    public static final List<UUID> STAFF = List.of(UUID.fromString("5e738102-a490-469f-b0f6-5f1fc2dbf70c"));

    private CuentasDemo() {
    }
}
