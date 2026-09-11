package com.helpdesk.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        AuthResponse user
) {
}
