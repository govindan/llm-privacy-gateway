package com.company.privacygateway.api;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeRequest(
        @NotBlank String conversationId,
        @NotBlank String text
) {
}
