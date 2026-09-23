package com.helpdesk.orgs;

import com.helpdesk.auth.Membership;
import com.helpdesk.auth.MembershipRepository;
import com.helpdesk.auth.MembershipRole;
import com.helpdesk.auth.RoleAuthorities;
import com.helpdesk.auth.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationContextInterceptorTest {

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private OrganizationContextInterceptor interceptor;

    @AfterEach
    void tearDown() {
        OrganizationContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void memberCanAccessOrganization() {
        interceptor = new OrganizationContextInterceptor(
                membershipRepository
        );

        UUID organizationId = UUID.randomUUID();
        User user = mock(User.class);
        Membership membership = mock(Membership.class);

        when(user.getId()).thenReturn(UUID.randomUUID());
        when(membership.getRole()).thenReturn(MembershipRole.AGENT);

        when(request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        )).thenReturn(
                Map.of("organizationId", organizationId.toString())
        );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(membershipRepository.findByUserIdAndOrganizationId(
                user.getId(),
                organizationId
        )).thenReturn(Optional.of(membership));

        boolean result = interceptor.preHandle(
                request,
                response,
                new Object()
        );

        assertTrue(result);

        assertEquals(
                organizationId,
                OrganizationContextHolder.get()
                        .getOrganizationId()
        );

        assertEquals(
                MembershipRole.AGENT,
                OrganizationContextHolder.get()
                        .getRole()
        );

        assertEquals(
                RoleAuthorities.authority(MembershipRole.AGENT).getAuthority(),
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
        );

        verify(membershipRepository)
                .findByUserIdAndOrganizationId(
                        user.getId(),
                        organizationId
                );
    }

    @Test
    void nonMemberIsRejectedWithForbidden() {
        interceptor = new OrganizationContextInterceptor(
                membershipRepository
        );

        UUID organizationId = UUID.randomUUID();
        User user = mock(User.class);

        when(user.getId()).thenReturn(UUID.randomUUID());

        when(request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        )).thenReturn(
                Map.of("organizationId", organizationId.toString())
        );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        Collections.emptyList()
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        when(membershipRepository.findByUserIdAndOrganizationId(
                user.getId(),
                organizationId
        )).thenReturn(Optional.empty());

        boolean result = interceptor.preHandle(
                request,
                response,
                new Object()
        );

        assertFalse(result);

        verify(response)
                .setStatus(HttpServletResponse.SC_FORBIDDEN);

        assertEquals(
                null,
                OrganizationContextHolder.get()
        );
    }

    @Test
    void unauthenticatedRequestIsRejectedWithUnauthorized() {
        interceptor = new OrganizationContextInterceptor(
                membershipRepository
        );

        UUID organizationId = UUID.randomUUID();

        when(request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        )).thenReturn(
                Map.of("organizationId", organizationId.toString())
        );

        SecurityContextHolder.clearContext();

        boolean result = interceptor.preHandle(
                request,
                response,
                new Object()
        );

        assertFalse(result);

        verify(response)
                .setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void invalidOrganizationIdIsRejectedWithBadRequest() {
        interceptor = new OrganizationContextInterceptor(
                membershipRepository
        );

        when(request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        )).thenReturn(
                Map.of("organizationId", "not-a-uuid")
        );

        boolean result = interceptor.preHandle(
                request,
                response,
                new Object()
        );

        assertFalse(result);

        verify(response)
                .setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
}
