package com.company.privacygateway.common;

public record ApiError(String code, String message, String correlationId) {
}
