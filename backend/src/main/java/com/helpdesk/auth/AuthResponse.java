package com.helpdesk.auth;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String email,
        String name
) {
}
