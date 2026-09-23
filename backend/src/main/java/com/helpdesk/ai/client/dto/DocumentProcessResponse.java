package com.helpdesk.ai.client.dto;

import java.util.List;

public record DocumentProcessResponse(
        List<DocumentChunkResponse> chunks
) {
}
