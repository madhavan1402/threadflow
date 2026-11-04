package com.threadflow.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.threadflow.user.Role;
import com.threadflow.user.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService(
            "test-only-jwt-signing-secret-with-at-least-32-characters",
            30
    );

    @Test
    void invalidJwtIsRejected() {
        assertThrows(JwtException.class, () -> jwtService.validateAndGetSubject("invalid.jwt.token"));
    }

    @Test
    void validJwtContainsEmailSubject() {
        User user = new User("user@threadflow.local", "unused", true);
        user.addRole(new Role("SUPERVISOR"));
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.validateAndGetSubject(token).equals("user@threadflow.local"));
    }

    @Test
    void expiredJwtIsRejected() throws InterruptedException {
        JwtService expiredService = new JwtService(
                "test-only-jwt-signing-secret-with-at-least-32-characters",
                0
        );
        User user = new User("user@threadflow.local", "unused", true);
        String token = expiredService.generateToken(user);
        Thread.sleep(20);
        assertThrows(JwtException.class, () -> expiredService.validateAndGetSubject(token));
    }
}
