package com.helpdesk.auth;

import java.util.UUID;

public record MemberSearchResponse(
        UUID userId,
        String name,
        String email
) {

    public static MemberSearchResponse from(User user) {
        return new MemberSearchResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
