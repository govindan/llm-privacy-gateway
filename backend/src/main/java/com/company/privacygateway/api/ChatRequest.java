package com.company.privacygateway.api;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String conversationId,
        @NotBlank String message
) {
}
