package com.helpdesk.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/members")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getMembers(
            @PathVariable UUID organizationId
    ) {
        return ResponseEntity.ok(
                membershipService.getMembers(organizationId)
        );
    }

    @PostMapping
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable UUID organizationId,
            @RequestBody AddMemberRequest request
    ) {
        MemberResponse response = membershipService.addMember(organizationId, request);
        return ResponseEntity.status(201).body(response);
    }
}
