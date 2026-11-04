package com.threadflow.auth;

import com.threadflow.user.User;
import com.threadflow.user.UserRepository;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResult login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return new LoginResult(jwtService.generateToken(user), currentUser(user));
    }

    public AuthDtos.CurrentUserResponse currentUser(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .sorted()
                .toList();
        return new AuthDtos.CurrentUserResponse(user.getId(), user.getEmail(), roles);
    }

    public record LoginResult(String token, AuthDtos.CurrentUserResponse user) {}
}
