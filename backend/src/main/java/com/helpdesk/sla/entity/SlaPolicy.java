package com.helpdesk.sla.entity;

import com.helpdesk.tickets.entity.TicketPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "sla_policies")
public class SlaPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "first_response_minutes", nullable = false)
    private Integer firstResponseMinutes;

    @Column(name = "resolution_minutes", nullable = false)
    private Integer resolutionMinutes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private TicketPriority priority;

    protected SlaPolicy() {
    }

    public SlaPolicy(
            UUID organizationId,
            String name,
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            TicketPriority priority
    ) {
        this.organizationId = organizationId;
        this.name = name;
        this.firstResponseMinutes = firstResponseMinutes;
        this.resolutionMinutes = resolutionMinutes;
        this.priority = priority;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public Integer getFirstResponseMinutes() {
        return firstResponseMinutes;
    }

    public Integer getResolutionMinutes() {
        return resolutionMinutes;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void update(
            String name,
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            TicketPriority priority
    ) {
        this.name = name;
        this.firstResponseMinutes = firstResponseMinutes;
        this.resolutionMinutes = resolutionMinutes;
        this.priority = priority;
    }
}
