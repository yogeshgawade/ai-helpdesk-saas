package com.helpdesk.kb.repository;

import java.util.UUID;

public record KbChunkSearchResult(
        UUID id,
        UUID documentId,
        UUID organizationId,
        String chunkText,
        int chunkIndex,
        Integer tokenCount,
        double similarity
) {
}
