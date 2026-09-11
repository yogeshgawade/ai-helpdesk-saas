package com.helpdesk.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByUserIdAndOrganizationId(
            UUID userId,
            UUID organizationId
    );

    boolean existsByUserIdAndOrganizationId(
            UUID userId,
            UUID organizationId
    );
}
