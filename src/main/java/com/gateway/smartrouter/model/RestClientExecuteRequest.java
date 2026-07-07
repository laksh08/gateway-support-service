package com.gateway.smartrouter.model;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record RestClientExecuteRequest(
        @NotBlank String method,
        @NotBlank String url,
        Map<String, String> headers,
        String body
) {
}
