package com.helpdesk.ai.client.dto;

public record ClassificationResponse(
        String category,
        String priority,
        double confidence,
        String reason
) {}
