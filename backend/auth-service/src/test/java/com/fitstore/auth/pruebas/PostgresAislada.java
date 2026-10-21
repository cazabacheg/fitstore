package com.fitstore.auth.pruebas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Una base de datos nueva y vacía en el PostgreSQL compartido, para pruebas que arrancan el servicio completo y
 * dependen de que no haya datos previos (admin inicial, datos de demostración).
 */
public class PostgresAislada implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        String nombre = "aislada_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection conexion = DriverManager.getConnection(PostgresPrueba.urlAdministracion(), PostgresPrueba.usuario(),
                PostgresPrueba.clave()); Statement sentencia = conexion.createStatement()) {
            sentencia.execute("CREATE DATABASE " + nombre);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo crear la base aislada", e);
        }
        TestPropertyValues.of(
                "spring.datasource.url=jdbc:postgresql://" + PostgresPrueba.host() + ":" + PostgresPrueba.puerto() + "/" + nombre,
                "spring.datasource.username=" + PostgresPrueba.usuario(),
                "spring.datasource.password=" + PostgresPrueba.clave()).applyTo(contexto);
    }
}
