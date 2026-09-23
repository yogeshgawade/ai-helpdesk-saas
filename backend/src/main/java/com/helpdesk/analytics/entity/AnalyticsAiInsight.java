package com.helpdesk.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "analytics_ai_insights")
public class AnalyticsAiInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String insight;

    @Column(nullable = false, length = 255)
    private String model;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AnalyticsAiInsight() {
    }

    public AnalyticsAiInsight(
            UUID organizationId,
            LocalDate fromDate,
            LocalDate toDate,
            String insight,
            String model
    ) {
        this.organizationId = organizationId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.insight = insight;
        this.model = model;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public String getInsight() {
        return insight;
    }

    public String getModel() {
        return model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
