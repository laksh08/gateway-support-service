package com.gateway.smartrouter.model;

public record AuthResponse(
        boolean authenticated,
        String username,
        String displayName,
        String message
) {
}
