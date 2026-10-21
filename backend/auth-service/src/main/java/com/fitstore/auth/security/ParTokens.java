package com.fitstore.auth.security;

import java.time.Instant;

/** Access y refresh token recién emitidos, con su vencimiento. */
public record ParTokens(String accessToken, Instant accessExpiraEn, String refreshToken, Instant refreshExpiraEn) {
}
