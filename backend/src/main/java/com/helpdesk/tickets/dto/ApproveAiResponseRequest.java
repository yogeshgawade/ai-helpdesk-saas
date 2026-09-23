package com.helpdesk.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ApproveAiResponseRequest(
        @NotNull
        UUID generationId,

        @NotBlank
        @Size(max = 10000)
        String body
) {}
