package com.helpdesk.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserId(UUID userId);

    @Query("""
            select m
            from Membership m
            join fetch m.user
            where m.organization.id = :organizationId
            """)
    List<Membership> findByOrganizationId(UUID organizationId);

    Optional<Membership> findByUserIdAndOrganizationId(
            UUID userId,
            UUID organizationId
    );

    boolean existsByUserIdAndOrganizationId(
            UUID userId,
            UUID organizationId
    );
}
