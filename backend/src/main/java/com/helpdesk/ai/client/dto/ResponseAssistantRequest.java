package com.helpdesk.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record ResponseAssistantRequest(
        @JsonProperty("ticket_subject")
        String ticketSubject,

        @JsonProperty("ticket_priority")
        String ticketPriority,

        @JsonProperty("ticket_category")
        String ticketCategory,

        List<ResponseAssistantMessage> messages,

        List<ResponseAssistantChunk> chunks
) {

    public record ResponseAssistantMessage(
            String body,

            @JsonProperty("internal_note")
            boolean internalNote
    ) {}

    public record ResponseAssistantChunk(
            String id,

            @JsonProperty("document_id")
            UUID documentId,

            @JsonProperty("document_title")
            String documentTitle,

            @JsonProperty("chunk_text")
            String chunkText,

            @JsonProperty("chunk_index")
            int chunkIndex,

            double similarity
    ) {}
}
