package com.helpdesk.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record RagContextChunk(
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
) {
}
