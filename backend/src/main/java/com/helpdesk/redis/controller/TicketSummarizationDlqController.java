package com.helpdesk.redis.controller;

import com.helpdesk.redis.TicketSummarizationDlqService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orgs/{organizationId}/admin/dlq")
public class TicketSummarizationDlqController {

    private final TicketSummarizationDlqService dlqService;

    public TicketSummarizationDlqController(
            TicketSummarizationDlqService dlqService
    ) {
        this.dlqService = dlqService;
    }

    @PostMapping("/ticket-summarization/reprocess")
    public Map<String, Object> reprocessTicketSummarization(
            @PathVariable UUID organizationId
    ) {
        int reprocessed =
                dlqService.reprocessForOrganization(organizationId);

        return Map.of(
                "reprocessed", reprocessed
        );
    }
}
