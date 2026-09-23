package com.helpdesk.orgs;

import com.helpdesk.auth.Membership;
import com.helpdesk.auth.MembershipRole;

import java.util.UUID;

public record OrganizationMembershipResponse(
        UUID id,
        String name,
        String slug,
        MembershipRole role
) {

    public static OrganizationMembershipResponse from(Membership membership) {
        Organization organization = membership.getOrganization();

        return new OrganizationMembershipResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                membership.getRole()
        );
    }
}
