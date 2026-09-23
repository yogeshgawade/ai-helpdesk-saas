package com.helpdesk.sla.controller;

import com.helpdesk.sla.dto.SlaPolicyRequest;
import com.helpdesk.sla.dto.SlaPolicyResponse;
import com.helpdesk.sla.entity.SlaPolicy;
import com.helpdesk.sla.service.SlaPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/sla-policies")
public class SlaPolicyController {

    private final SlaPolicyService slaPolicyService;

    public SlaPolicyController(
            SlaPolicyService slaPolicyService
    ) {
        this.slaPolicyService = slaPolicyService;
    }

    @GetMapping
    public List<SlaPolicyResponse> getPolicies(
            @PathVariable UUID organizationId
    ) {
        return slaPolicyService
                .getPolicies(organizationId)
                .stream()
                .map(SlaPolicyResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlaPolicyResponse createPolicy(
            @PathVariable UUID organizationId,
            @Valid @RequestBody SlaPolicyRequest request
    ) {
        SlaPolicy policy =
                slaPolicyService.createPolicy(
                        organizationId,
                        request.name(),
                        request.firstResponseMinutes(),
                        request.resolutionMinutes(),
                        request.priority()
                );

        return SlaPolicyResponse.from(policy);
    }

    @PutMapping("/{policyId}")
    public SlaPolicyResponse updatePolicy(
            @PathVariable UUID organizationId,
            @PathVariable UUID policyId,
            @Valid @RequestBody SlaPolicyRequest request
    ) {
        SlaPolicy policy =
                slaPolicyService.updatePolicy(
                        organizationId,
                        policyId,
                        request.name(),
                        request.firstResponseMinutes(),
                        request.resolutionMinutes(),
                        request.priority()
                );

        return SlaPolicyResponse.from(policy);
    }

    @DeleteMapping("/{policyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePolicy(
            @PathVariable UUID organizationId,
            @PathVariable UUID policyId
    ) {
        slaPolicyService.deletePolicy(
                organizationId,
                policyId
        );
    }
}
