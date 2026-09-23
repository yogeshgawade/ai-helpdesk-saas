package com.helpdesk.ai.client.dto;

import java.util.List;

public record RagRequest(
        String query,
        List<RagContextChunk> chunks
) {
}
