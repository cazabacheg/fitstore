package com.fitstore.notification.pruebas;

import com.fitstore.notification.envio.Pausa;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Reloj simulado (es el {@code Clock} del contexto) y pausa que solo anota cuánto se habría esperado entre correos. */
@TestConfiguration
public class ConfiguracionDeTiempo {

    @Bean
    @Primary
    public RelojDePrueba relojDePrueba() {
        return new RelojDePrueba();
    }

    @Bean
    @Primary
    public PausaRegistrada pausaRegistrada() {
        return new PausaRegistrada();
    }

    public static class PausaRegistrada implements Pausa {

        private final List<Long> esperas = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void esperar(long milisegundos) {
            esperas.add(milisegundos);
        }

        public List<Long> esperas() {
            return new ArrayList<>(esperas);
        }

        public void limpiar() {
            esperas.clear();
        }
    }
}
