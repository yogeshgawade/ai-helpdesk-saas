package com.helpdesk.orgs;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/test")
public class OrganizationTestController {

    @GetMapping
    public Map<String, Object> testOrganizationContext(
            @PathVariable UUID organizationId) {

        OrganizationContext context =
                OrganizationContextHolder.get();

        return Map.of(
                "organizationId", context.getOrganizationId(),
                "role", context.getRole()
        );
    }

    @GetMapping("/owner-only")
    @PreAuthorize("hasRole('OWNER')")
    public Map<String, String> ownerOnly() {
        return Map.of("message", "OWNER access granted");
    }

    @GetMapping("/agent-or-above")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'AGENT')")
    public Map<String, String> agentOrAbove() {
        return Map.of("message", "Agent-level access granted");
    }
}
