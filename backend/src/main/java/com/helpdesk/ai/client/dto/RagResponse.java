package com.helpdesk.ai.client.dto;

import java.util.List;

public record RagResponse(
        String answer,
        List<RagCitation> citations
) {
}
