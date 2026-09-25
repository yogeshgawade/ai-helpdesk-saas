package com.helpdesk.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
            select u
            from User u
            where (
                lower(u.email) like lower(concat('%', :query, '%'))
                or lower(u.name) like lower(concat('%', :query, '%'))
            )
            and u.id not in (
                select m.user.id
                from Membership m
                where m.organization.id = :organizationId
            )
            order by u.name asc
            """)
    List<User> searchAvailableUsers(
            @Param("query") String query,
            @Param("organizationId") UUID organizationId
    );
}
