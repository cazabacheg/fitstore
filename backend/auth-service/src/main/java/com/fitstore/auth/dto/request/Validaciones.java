package com.fitstore.auth.dto.request;

/** Expresiones y mensajes de validación compartidos por los DTO de entrada (micro 4.7, contrato de comun.yaml). */
public final class Validaciones {

    /** 8 a 64 caracteres, al menos una letra y un número. */
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*[0-9]).{8,64}$";
    public static final String MENSAJE_PASSWORD = "La contraseña debe tener de 8 a 64 caracteres, con al menos una letra y un número";

    public static final String DNI = "^[0-9]{8}$";
    public static final String MENSAJE_DNI = "El DNI debe tener 8 dígitos";

    /** Celular peruano: 9 dígitos que empiezan con 9. */
    public static final String CELULAR = "^9[0-9]{8}$";
    public static final String MENSAJE_CELULAR = "El celular debe tener 9 dígitos y empezar con 9";

    public static final String UBIGEO = "^[0-9]{6}$";
    public static final String MENSAJE_UBIGEO = "El ubigeo debe tener 6 dígitos";

    public static final String MENSAJE_EMAIL = "Ingresa un correo válido";
    public static final String MENSAJE_NOMBRE = "Debe tener entre 2 y 80 caracteres";

    private Validaciones() {
    }
}
