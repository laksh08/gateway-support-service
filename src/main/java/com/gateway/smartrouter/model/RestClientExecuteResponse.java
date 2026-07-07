package com.gateway.smartrouter.model;

import java.util.Map;

public record RestClientExecuteResponse(
        int statusCode,
        Map<String, String> headers,
        String body,
        long durationMs
) {
}
