package com.helpdesk.auth;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID membershipId,
        UUID userId,
        String name,
        String email,
        UUID organizationId,
        MembershipRole role,
        Instant createdAt
) {

    public static MemberResponse from(Membership membership) {
        return new MemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getName(),
                membership.getUser().getEmail(),
                membership.getOrganization().getId(),
                membership.getRole(),
                membership.getCreatedAt()
        );
    }
}
