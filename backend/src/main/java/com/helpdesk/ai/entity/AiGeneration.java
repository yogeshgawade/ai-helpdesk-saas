package com.helpdesk.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_generations")
public class AiGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Kind kind;

    @Column(name = "prompt_hash", length = 128)
    private String promptHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String output;

    @Column(nullable = false, length = 255)
    private String model;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiGeneration() {
    }

    public AiGeneration(
            UUID ticketId,
            Kind kind,
            String output,
            String model,
            Long latencyMs
    ) {
        this.ticketId = ticketId;
        this.kind = kind;
        this.output = output;
        this.model = model;
        this.latencyMs = latencyMs;
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

    public UUID getTicketId() {
        return ticketId;
    }

    public Kind getKind() {
        return kind;
    }

    public String getPromptHash() {
        return promptHash;
    }

    public String getOutput() {
        return output;
    }

    public String getModel() {
        return model;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void approve(UUID userId) {
        this.approvedBy = userId;
    }

    public enum Kind {
        RESPONSE_SUGGESTION,
        SUMMARY,
        INSIGHT
    }
}
