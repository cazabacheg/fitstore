package com.fitstore.auth.security;

import com.fitstore.auth.config.PropiedadesAuth;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** Cookie del refresh token (A4): {@code HttpOnly}, {@code SameSite=Strict}, ruta {@code /api/v1/auth}, {@code Secure} salvo en dev. */
@Component
public class CookieRefresh {

    private final PropiedadesAuth.Cookie config;

    private final Clock reloj;

    public CookieRefresh(PropiedadesAuth propiedades, Clock reloj) {
        this.config = propiedades.cookie();
        this.reloj = reloj;
    }

    /** Valor del encabezado {@code Set-Cookie} que entrega el refresh token; vive lo que le queda al token. */
    public String crear(ParTokens tokens) {
        Duration vida = Duration.between(reloj.instant(), tokens.refreshExpiraEn());
        return base(tokens.refreshToken()).maxAge(vida).build().toString();
    }

    /** Valor del encabezado {@code Set-Cookie} que borra la cookie. */
    public String borrar() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    /** Refresh token que llegó en la cookie, o {@code null}. */
    public String leer(HttpServletRequest peticion) {
        Cookie[] cookies = peticion.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (config.nombre().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(config.nombre(), valor)
                .httpOnly(true)
                .secure(config.secure())
                .sameSite(config.sameSite())
                .path(config.path());
    }
}
