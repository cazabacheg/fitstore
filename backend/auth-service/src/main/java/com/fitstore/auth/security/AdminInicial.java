package com.fitstore.auth.security;

import com.fitstore.auth.repository.StaffRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea el primer ADMIN al arrancar desde {@code FITSTORE_ADMIN_EMAIL} y {@code FITSTORE_ADMIN_PASSWORD} (D6, CA-1-38). */
@Component
public class AdminInicial implements ApplicationRunner {

    public AdminInicial(StaffRepository personal, PasswordEncoder codificador, Clock reloj,
            @Value("${FITSTORE_ADMIN_EMAIL:}") String email, @Value("${FITSTORE_ADMIN_PASSWORD:}") String password) {
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        // pendiente
    }
}
