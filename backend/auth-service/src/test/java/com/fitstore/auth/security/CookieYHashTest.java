package com.fitstore.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fitstore.auth.config.PropiedadesAuth;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Cookie de refresh (A4) y tokens de contraseña. */
class CookieYHashTest {

    private final PropiedadesAuth propiedades = new PropiedadesAuth("http://localhost:8080", Duration.ofMinutes(15),
            Duration.ofDays(7), 4, new PropiedadesAuth.Bloqueo(5, Duration.ofMinutes(15)), Duration.ofMinutes(30),
            Duration.ofHours(24), new PropiedadesAuth.Claves(null, null, "fs-test", true),
            new PropiedadesAuth.Cookie("fs_refresh", true, "Strict", "/api/v1/auth"),
            new PropiedadesAuth.Enlaces("http://tienda", "http://admin"));
    private final Instant ahora = Instant.parse("2026-10-21T15:00:00Z");
    private final CookieRefresh cookie = new CookieRefresh(propiedades, Clock.fixed(ahora, ZoneOffset.UTC));

    @Test
    @DisplayName("CA-1-04 la cookie de refresh es HttpOnly, Secure, SameSite=Strict, de ruta /api/v1/auth y vive lo que el token")
    void atributosDeLaCookie() {
        ParTokens par = new ParTokens("a", ahora.plusSeconds(900), "refresh-token", ahora.plus(Duration.ofDays(7)));

        String valor = cookie.crear(par);

        assertThat(valor).startsWith("fs_refresh=refresh-token").contains("HttpOnly", "Secure", "SameSite=Strict",
                "Path=/api/v1/auth", "Max-Age=604800");
    }

    @Test
    @DisplayName("CA-1-10 la cookie de borrado vence de inmediato")
    void cookieDeBorrado() {
        assertThat(cookie.borrar()).startsWith("fs_refresh=;").contains("Max-Age=0", "HttpOnly");
    }

    @Test
    @DisplayName("CA-1-08 lee el refresh de su cookie e ignora las demás")
    void leeLaCookie() {
        MockHttpServletRequest sinCookies = new MockHttpServletRequest();
        MockHttpServletRequest otraCookie = new MockHttpServletRequest();
        otraCookie.setCookies(new Cookie("otra", "x"));
        MockHttpServletRequest conCookie = new MockHttpServletRequest();
        conCookie.setCookies(new Cookie("otra", "x"), new Cookie("fs_refresh", "valor"));

        assertThat(cookie.leer(sinCookies)).isNull();
        assertThat(cookie.leer(otraCookie)).isNull();
        assertThat(cookie.leer(conCookie)).isEqualTo("valor");
    }

    @Test
    @DisplayName("CA-1-12 el token de contraseña tiene 43 caracteres aleatorios y su hash SHA-256 son 64 hexadecimales")
    void tokensDeContrasena() {
        String a = HashToken.generar();
        String b = HashToken.generar();

        assertThat(a).hasSize(43).isNotEqualTo(b).matches("[A-Za-z0-9_-]+");
        assertThat(HashToken.sha256(a)).hasSize(64).matches("[0-9a-f]+").isNotEqualTo(HashToken.sha256(b));
        assertThat(HashToken.sha256("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
