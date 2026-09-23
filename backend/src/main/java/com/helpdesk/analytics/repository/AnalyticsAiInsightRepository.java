package com.helpdesk.analytics.repository;

import com.helpdesk.analytics.entity.AnalyticsAiInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalyticsAiInsightRepository
        extends JpaRepository<AnalyticsAiInsight, UUID> {

    List<AnalyticsAiInsight>
    findByOrganizationIdOrderByCreatedAtDesc(
            UUID organizationId
    );

    Optional<AnalyticsAiInsight>
    findFirstByOrganizationIdAndFromDateAndToDateOrderByCreatedAtDesc(
            UUID organizationId,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate
    );
}
