package com.helpdesk.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record ResponseAssistantResponse(
        String answer,
        String model,
        List<ResponseAssistantCitation> citations
) {
    public record ResponseAssistantCitation(
            @JsonProperty("document_id")
            UUID documentId,

            @JsonProperty("document_title")
            String documentTitle,

            @JsonProperty("chunk_id")
            UUID chunkId,

            @JsonProperty("chunk_index")
            int chunkIndex
    ) {}
}
