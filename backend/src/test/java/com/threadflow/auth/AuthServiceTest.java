package com.threadflow.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.threadflow.user.Role;
import com.threadflow.user.User;
import com.threadflow.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(
            "test-only-jwt-signing-secret-with-at-least-32-characters",
            30
    );

    @Test
    void successfulLoginReturnsUserAndToken() {
        User user = user("merch@threadflow.local", "correct-password", true);
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("merch@threadflow.local")).thenReturn(Optional.of(user));
        AuthService service = new AuthService(repository, encoder, jwtService);

        AuthService.LoginResult result = service.login(new AuthDtos.LoginRequest(
                "merch@threadflow.local",
                "correct-password"
        ));

        assertEquals("merch@threadflow.local", result.user().email());
        assertEquals("MERCHANDISER", result.user().roles().getFirst());
        assertEquals("merch@threadflow.local", jwtService.validateAndGetSubject(result.token()));
    }

    @Test
    void incorrectPasswordIsRejected() {
        User user = user("merch@threadflow.local", "correct-password", true);
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("merch@threadflow.local")).thenReturn(Optional.of(user));
        AuthService service = new AuthService(repository, encoder, jwtService);

        assertThrows(BadCredentialsException.class, () -> service.login(
                new AuthDtos.LoginRequest("merch@threadflow.local", "wrong-password")));
    }

    @Test
    void unknownEmailIsRejected() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("missing@threadflow.local")).thenReturn(Optional.empty());
        AuthService service = new AuthService(repository, encoder, jwtService);

        assertThrows(BadCredentialsException.class, () -> service.login(
                new AuthDtos.LoginRequest("missing@threadflow.local", "correct-password")));
    }

    @Test
    void disabledAccountIsRejected() {
        User user = user("disabled@threadflow.local", "correct-password", false);
        UserRepository repository = mock(UserRepository.class);
        when(repository.findByEmailIgnoreCase("disabled@threadflow.local")).thenReturn(Optional.of(user));
        AuthService service = new AuthService(repository, encoder, jwtService);

        assertThrows(BadCredentialsException.class, () -> service.login(
                new AuthDtos.LoginRequest("disabled@threadflow.local", "correct-password")));
    }

    private User user(String email, String password, boolean enabled) {
        User user = new User(email, encoder.encode(password), enabled);
        user.addRole(new Role("MERCHANDISER"));
        return user;
    }
}
