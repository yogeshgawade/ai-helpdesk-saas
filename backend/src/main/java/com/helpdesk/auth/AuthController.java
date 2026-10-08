package com.helpdesk.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final long refreshTokenExpirationMs;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;

    public AuthController(
            AuthService authService,
            @Value("${app.auth.refresh-token-expiration-ms}") long refreshTokenExpirationMs,
            @Value("${app.auth.refresh-cookie-secure:false}") boolean refreshCookieSecure,
            @Value("${app.auth.refresh-cookie-same-site:Lax}") String refreshCookieSameSite) {
        this.authService = authService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthService.TokenSession session = authService.register(request);
        setRefreshCookie(response, session.refreshToken());
        return session.loginResponse();
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthService.TokenSession session = authService.login(request);
        setRefreshCookie(response, session.refreshToken());
        return session.loginResponse();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        AuthService.TokenSession session = authService.refresh(refreshToken);
        setRefreshCookie(response, session.refreshToken());
        return ResponseEntity.ok(new AccessTokenResponse(session.loginResponse().accessToken(), "Bearer"));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = RefreshTokenService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
    }

    @PostMapping("/websocket-ticket")
    public WebSocketTicketResponse websocketTicket(
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();

        return new WebSocketTicketResponse(
                authService.createWebSocketTicket(user)
        );
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader("Set-Cookie", ResponseCookie.from(RefreshTokenService.COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/auth")
                .maxAge(Duration.ofMillis(refreshTokenExpirationMs))
                .build()
                .toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", ResponseCookie.from(RefreshTokenService.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build()
                .toString());
    }
}
