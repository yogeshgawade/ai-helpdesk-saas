package com.helpdesk.orgs;

import com.helpdesk.auth.Membership;
import com.helpdesk.auth.MembershipRepository;
import com.helpdesk.auth.RoleAuthorities;
import com.helpdesk.auth.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
public class OrganizationContextInterceptor implements HandlerInterceptor {

    private final MembershipRepository membershipRepository;

    public OrganizationContextInterceptor(
            MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables =
                (Map<String, String>) request.getAttribute(
                        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
                );

        if (pathVariables == null ||
                !pathVariables.containsKey("organizationId")) {
            return true;
        }

        UUID organizationId;

        try {
            organizationId = UUID.fromString(
                    pathVariables.get("organizationId")
            );
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof User user)) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        Membership membership =
                membershipRepository
                        .findByUserIdAndOrganizationId(
                                user.getId(),
                                organizationId
                        )
                        .orElse(null);

        if (membership == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }

        OrganizationContextHolder.set(
                new OrganizationContext(
                        organizationId,
                        membership.getRole()
                )
        );

        TenantDatabaseContextHolder.set(organizationId);

        UsernamePasswordAuthenticationToken authenticatedUser =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        Collections.singleton(
                                RoleAuthorities.authority(
                                        membership.getRole()
                                )
                        )
                );

        authenticatedUser.setDetails(
                authentication.getDetails()
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authenticatedUser);

        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception) {

        OrganizationContextHolder.clear();
        TenantDatabaseContextHolder.clear();
    }
}
