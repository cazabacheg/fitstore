package com.fitstore.common.persistence;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link FormatMapper} de Hibernate para columnas JSON ({@code jsonb}) sobre Jackson 3, que es el que usa Spring
 * Boot 4 (Hibernate 7.2 solo trae soporte para Jackson 2). Un atributo {@code String} se guarda tal cual (ya es el
 * documento JSON); cualquier otro tipo se serializa con Jackson.
 */
public class JacksonJsonFormatMapper implements FormatMapper {

    private final JsonMapper json;

    public JacksonJsonFormatMapper(JsonMapper json) {
        this.json = json;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T fromString(CharSequence cadena, JavaType<T> tipo, WrapperOptions opciones) {
        if (tipo.getJavaType() == String.class || tipo.getJavaType() == CharSequence.class) {
            return (T) cadena.toString();
        }
        return json.readValue(cadena.toString(), json.constructType(tipo.getJavaType()));
    }

    @Override
    public <T> String toString(T valor, JavaType<T> tipo, WrapperOptions opciones) {
        if (valor instanceof CharSequence texto && (tipo.getJavaType() == String.class
                || tipo.getJavaType() == CharSequence.class)) {
            return texto.toString();
        }
        return json.writeValueAsString(valor);
    }
}
