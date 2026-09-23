package com.helpdesk.ai.client.dto;

import java.util.List;

public record SummarizationRequest(
        String subject,
        List<SummarizationMessage> messages
) {}
