package com.helpdesk.orgs;

import com.helpdesk.auth.MembershipRole;

import java.util.UUID;

public class OrganizationContext {

    private final UUID organizationId;
    private final MembershipRole role;

    public OrganizationContext(
            UUID organizationId,
            MembershipRole role) {

        this.organizationId = organizationId;
        this.role = role;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public MembershipRole getRole() {
        return role;
    }
}
