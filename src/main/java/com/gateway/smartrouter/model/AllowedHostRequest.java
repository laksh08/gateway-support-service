package com.gateway.smartrouter.model;

import jakarta.validation.constraints.NotBlank;

public record AllowedHostRequest(
        @NotBlank String hostname,
        String ip,
        String description,
        String dmzServer,
        @NotBlank String status
) {
}
