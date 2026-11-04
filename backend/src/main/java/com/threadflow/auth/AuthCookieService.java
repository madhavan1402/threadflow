package com.threadflow.auth;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {
    public static final String AUTH_COOKIE = "THREADFLOW_AUTH";

    private final boolean secure;
    private final String sameSite;
    private final long expirationMinutes;

    public AuthCookieService(
            @Value("${threadflow.cookie.secure:true}") boolean secure,
            @Value("${threadflow.cookie.same-site:Lax}") String sameSite,
            @Value("${threadflow.jwt.expiration-minutes:480}") long expirationMinutes
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.expirationMinutes = expirationMinutes;
    }

    public ResponseCookie create(String token) {
        return ResponseCookie.from(AUTH_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofMinutes(expirationMinutes))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(AUTH_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    public ResponseCookie clearCsrf() {
        return ResponseCookie.from("XSRF-TOKEN", "")
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
