package com.helpdesk.sla.service;

import com.helpdesk.sla.entity.SlaPolicy;
import com.helpdesk.sla.repository.SlaPolicyRepository;
import com.helpdesk.tickets.entity.TicketPriority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SlaPolicyService {

    private final SlaPolicyRepository slaPolicyRepository;

    public SlaPolicyService(
            SlaPolicyRepository slaPolicyRepository
    ) {
        this.slaPolicyRepository = slaPolicyRepository;
    }

    @Transactional(readOnly = true)
    public List<SlaPolicy> getPolicies(UUID organizationId) {
        return slaPolicyRepository
                .findByOrganizationIdOrderByPriorityAsc(
                        organizationId
                );
    }

    @Transactional
    public SlaPolicy createPolicy(
            UUID organizationId,
            String name,
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            TicketPriority priority
    ) {
        validateDurations(
                firstResponseMinutes,
                resolutionMinutes
        );

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "SLA policy name is required"
            );
        }

        if (priority == null) {
            throw new IllegalArgumentException(
                    "SLA policy priority is required"
            );
        }

        if (slaPolicyRepository
                .findByOrganizationIdAndPriority(
                        organizationId,
                        priority
                )
                .isPresent()) {

            throw new IllegalArgumentException(
                    "An SLA policy already exists for this priority"
            );
        }

        SlaPolicy policy = new SlaPolicy(
                organizationId,
                name.trim(),
                firstResponseMinutes,
                resolutionMinutes,
                priority
        );

        return slaPolicyRepository.save(policy);
    }

    @Transactional
    public SlaPolicy updatePolicy(
            UUID organizationId,
            UUID policyId,
            String name,
            Integer firstResponseMinutes,
            Integer resolutionMinutes,
            TicketPriority priority
    ) {
        validateDurations(
                firstResponseMinutes,
                resolutionMinutes
        );

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "SLA policy name is required"
            );
        }

        if (priority == null) {
            throw new IllegalArgumentException(
                    "SLA policy priority is required"
            );
        }

        SlaPolicy policy =
                slaPolicyRepository
                        .findByIdAndOrganizationId(
                                policyId,
                                organizationId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "SLA policy not found"
                                )
                        );

        slaPolicyRepository
                .findByOrganizationIdAndPriority(
                        organizationId,
                        priority
                )
                .filter(existing ->
                        !existing.getId().equals(policyId)
                )
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "An SLA policy already exists for this priority"
                    );
                });

        policy.update(
                name.trim(),
                firstResponseMinutes,
                resolutionMinutes,
                priority
        );

        return slaPolicyRepository.save(policy);
    }

    @Transactional
    public void deletePolicy(
            UUID organizationId,
            UUID policyId
    ) {
        SlaPolicy policy =
                slaPolicyRepository
                        .findByIdAndOrganizationId(
                                policyId,
                                organizationId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "SLA policy not found"
                                )
                        );

        slaPolicyRepository.delete(policy);
    }

    private void validateDurations(
            Integer firstResponseMinutes,
            Integer resolutionMinutes
    ) {
        if (firstResponseMinutes == null
                || firstResponseMinutes <= 0) {
            throw new IllegalArgumentException(
                    "First response time must be greater than zero"
            );
        }

        if (resolutionMinutes == null
                || resolutionMinutes <= 0) {
            throw new IllegalArgumentException(
                    "Resolution time must be greater than zero"
            );
        }
    }
}
