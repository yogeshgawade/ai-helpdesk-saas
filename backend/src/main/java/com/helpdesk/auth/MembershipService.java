package com.helpdesk.auth;

import com.helpdesk.exception.ForbiddenException;
import com.helpdesk.orgs.Organization;
import com.helpdesk.orgs.OrganizationContext;
import com.helpdesk.orgs.OrganizationContextHolder;
import com.helpdesk.orgs.OrganizationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public MembershipService(
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository
    ) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    public java.util.List<MemberResponse> getMembers(UUID organizationId) {
        OrganizationContext context = OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException("Organization context not set");
        }

        if (!context.getOrganizationId().equals(organizationId)) {
            throw new ForbiddenException(
                    "Organization does not match the current context"
            );
        }

        return membershipRepository.findByOrganizationId(
                        context.getOrganizationId()
                )
                .stream()
                .map(MemberResponse::from)
                .toList();
    }

    @Transactional
    public MemberResponse addMember(
            UUID organizationId,
            AddMemberRequest request
    ) {
        OrganizationContext context = OrganizationContextHolder.get();

        if (context == null) {
            throw new IllegalStateException("Organization context not set");
        }

        if (!context.getOrganizationId().equals(organizationId)) {
            throw new ForbiddenException(
                    "Organization does not match the current context"
            );
        }

        MembershipRole currentRole = context.getRole();

        if (currentRole != MembershipRole.OWNER
                && currentRole != MembershipRole.ADMIN) {
            throw new ForbiddenException(
                    "Only owners and admins can add members"
            );
        }

        if (request.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }

        if (request.role() == null) {
            throw new IllegalArgumentException("role is required");
        }

        if (request.role() == MembershipRole.OWNER) {
            throw new ForbiddenException("Cannot add a member with OWNER role");
        }

        if (request.role() == MembershipRole.ADMIN
                && currentRole != MembershipRole.OWNER) {
            throw new ForbiddenException("Only owners can add admins");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UUID currentOrganizationId = context.getOrganizationId();

        if (membershipRepository.existsByUserIdAndOrganizationId(
                user.getId(),
                currentOrganizationId
        )) {
            throw new IllegalArgumentException(
                    "User is already a member of this organization"
            );
        }

        Organization organization = organizationRepository.findById(
                        currentOrganizationId
                )
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        Membership membership = new Membership(
                user,
                organization,
                request.role()
        );

        Membership savedMembership = membershipRepository.save(membership);

        return MemberResponse.from(savedMembership);
    }
}
