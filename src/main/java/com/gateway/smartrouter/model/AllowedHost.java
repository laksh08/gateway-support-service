package com.gateway.smartrouter.model;

import java.time.Instant;

public record AllowedHost(
        Long id,
        String hostname,
        String ip,
        String description,
        String dmzServer,
        String status,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
