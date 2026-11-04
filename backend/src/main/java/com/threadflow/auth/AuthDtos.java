package com.threadflow.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address")
            @Size(max = 255)
            String email,
            @NotBlank(message = "Password is required")
            @Size(max = 255)
            String password
    ) {}

    public record CurrentUserResponse(Long id, String email, List<String> roles) {}

    public record LogoutResponse(String status) {}

    public record CsrfResponse(String headerName, String token) {}
}
