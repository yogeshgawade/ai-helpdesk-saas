package com.helpdesk.redis.controller;

import com.helpdesk.redis.TicketClassificationDlqService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/admin/dlq")
public class TicketClassificationDlqController {

    private final TicketClassificationDlqService dlqService;

    public TicketClassificationDlqController(
            TicketClassificationDlqService dlqService
    ) {
        this.dlqService = dlqService;
    }

    @PostMapping("/ticket-classification/reprocess")
    public Map<String, Object> reprocessTicketClassification(
            @PathVariable UUID organizationId
    ) {
        int reprocessed =
                dlqService.reprocessForOrganization(organizationId);

        return Map.of(
                "reprocessed", reprocessed
        );
    }
}
