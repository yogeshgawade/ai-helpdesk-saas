package com.helpdesk.ai.client.dto;

public record ClassificationRequest(
        String subject,
        String priority,
        String category
) {}
