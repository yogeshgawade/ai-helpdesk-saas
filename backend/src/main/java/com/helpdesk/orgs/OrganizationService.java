package com.helpdesk.orgs;

import com.helpdesk.auth.Membership;
import com.helpdesk.auth.MembershipRepository;
import com.helpdesk.auth.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationService {

    private final MembershipRepository membershipRepository;

    public OrganizationService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public List<OrganizationMembershipResponse> getCurrentUserOrganizations() {
        User user = getCurrentUser();

        return membershipRepository.findByUserId(user.getId())
                .stream()
                .map(OrganizationMembershipResponse::from)
                .toList();
    }

    private User getCurrentUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof User user)) {
            throw new IllegalStateException("User not authenticated");
        }

        return user;
    }
}
