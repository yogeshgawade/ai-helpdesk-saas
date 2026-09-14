package com.helpdesk.auth;

import java.util.UUID;

public record AddMemberRequest(
        UUID userId,
        MembershipRole role
) {
}
