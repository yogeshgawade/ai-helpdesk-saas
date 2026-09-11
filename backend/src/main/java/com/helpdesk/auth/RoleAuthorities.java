package com.helpdesk.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class RoleAuthorities {

    private RoleAuthorities() {
    }

    public static GrantedAuthority authority(MembershipRole role) {
        return new SimpleGrantedAuthority("ROLE_" + role.name());
    }
}
