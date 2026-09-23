package com.helpdesk.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DocumentChunkResponse(
        String text,

        @JsonProperty("chunk_index")
        int chunkIndex,

        @JsonProperty("token_count")
        int tokenCount
) {
}
