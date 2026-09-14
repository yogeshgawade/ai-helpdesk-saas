package com.helpdesk.ai.client.dto;

import java.util.List;

public record EmbeddingResponse(
        List<Float> embedding,
        int dimension
) {
}
