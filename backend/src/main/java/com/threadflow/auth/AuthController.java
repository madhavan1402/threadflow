package com.threadflow.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.threadflow.user.User;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService cookieService;

    public AuthController(AuthService authService, AuthCookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @GetMapping("/csrf")
    public AuthDtos.CsrfResponse csrf(CsrfToken token) {
        return new AuthDtos.CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.CurrentUserResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.create(result.token()).toString())
                .body(result.user());
    }

    @GetMapping("/me")
    public AuthDtos.CurrentUserResponse me(@AuthenticationPrincipal User user) {
        return authService.currentUser(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthDtos.LogoutResponse> logout() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookieService.clear().toString());
        headers.add(HttpHeaders.SET_COOKIE, cookieService.clearCsrf().toString());
        return ResponseEntity.ok()
                .headers(headers)
                .body(new AuthDtos.LogoutResponse("LOGGED_OUT"));
    }
}
