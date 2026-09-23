package com.helpdesk.tickets.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "assigned_agent_id")
    private UUID assignedAgentId;

    @Column(nullable = false, length = 500)
    private String subject;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TicketPriority priority;

    @Column(length = 100)
    private String category;

    @Column(name = "ai_category", length = 100)
    private String aiCategory;

    @Column(name = "ai_priority", length = 20)
    private String aiPriority;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "ai_reason", columnDefinition = "TEXT")
    private String aiReason;

    @Column(name = "ai_classified_at")
    private Instant aiClassifiedAt;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_summarized_at")
    private Instant aiSummarizedAt;

    @Column(name = "sla_policy_id")
    private UUID slaPolicyId;

    @Column(name = "first_response_due_at")
    private Instant firstResponseDueAt;

    @Column(name = "resolution_due_at")
    private Instant resolutionDueAt;

    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;

    @Column(name = "sla_first_response_breached", nullable = false)
    private boolean slaFirstResponseBreached;

    @Column(name = "sla_resolution_breached", nullable = false)
    private boolean slaResolutionBreached;

    @Column(name = "sla_first_response_breached_at")
    private Instant slaFirstResponseBreachedAt;

    @Column(name = "sla_resolution_breached_at")
    private Instant slaResolutionBreachedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Ticket() {
    }

    public Ticket(
            UUID organizationId,
            UUID customerId,
            String subject,
            TicketPriority priority,
            String category
    ) {
        this.organizationId = organizationId;
        this.customerId = customerId;
        this.subject = subject;
        this.status = TicketStatus.OPEN;
        this.priority = priority != null ? priority : TicketPriority.MEDIUM;
        this.category = category;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null) {
            status = TicketStatus.OPEN;
        }

        if (priority == null) {
            priority = TicketPriority.MEDIUM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();

        if (status == TicketStatus.RESOLVED && resolvedAt == null) {
            resolvedAt = Instant.now();
        }

        if (status != TicketStatus.RESOLVED) {
            resolvedAt = null;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getAssignedAgentId() {
        return assignedAgentId;
    }

    public String getSubject() {
        return subject;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public String getCategory() {
        return category;
    }

    public UUID getSlaPolicyId() {
        return slaPolicyId;
    }

    public Instant getFirstResponseDueAt() {
        return firstResponseDueAt;
    }

    public Instant getResolutionDueAt() {
        return resolutionDueAt;
    }

    public Instant getFirstRespondedAt() {
        return firstRespondedAt;
    }

    public boolean isSlaFirstResponseBreached() {
        return slaFirstResponseBreached;
    }

    public boolean isSlaResolutionBreached() {
        return slaResolutionBreached;
    }

    public Instant getSlaFirstResponseBreachedAt() {
        return slaFirstResponseBreachedAt;
    }

    public Instant getSlaResolutionBreachedAt() {
        return slaResolutionBreachedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getAiCategory() {
        return aiCategory;
    }

    public String getAiPriority() {
        return aiPriority;
    }

    public Double getAiConfidence() {
        return aiConfidence;
    }

    public String getAiReason() {
        return aiReason;
    }

    public Instant getAiClassifiedAt() {
        return aiClassifiedAt;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public Instant getAiSummarizedAt() {
        return aiSummarizedAt;
    }

    public void setAssignedAgentId(UUID assignedAgentId) {
        this.assignedAgentId = assignedAgentId;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setSlaPolicyId(UUID slaPolicyId) {
        this.slaPolicyId = slaPolicyId;
    }

    public void setFirstResponseDueAt(Instant firstResponseDueAt) {
        this.firstResponseDueAt = firstResponseDueAt;
    }

    public void setResolutionDueAt(Instant resolutionDueAt) {
        this.resolutionDueAt = resolutionDueAt;
    }

    public void markFirstResponded(Instant respondedAt) {
        if (this.firstRespondedAt == null) {
            this.firstRespondedAt = respondedAt;
        }
    }

    public void markFirstResponseBreached(Instant breachedAt) {
        if (!this.slaFirstResponseBreached) {
            this.slaFirstResponseBreached = true;
            this.slaFirstResponseBreachedAt = breachedAt;
        }
    }

    public void markResolutionBreached(Instant breachedAt) {
        if (!this.slaResolutionBreached) {
            this.slaResolutionBreached = true;
            this.slaResolutionBreachedAt = breachedAt;
        }
    }
}
