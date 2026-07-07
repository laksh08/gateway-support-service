package com.gateway.smartrouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forwarding")
public record ForwardingProperties(
        String serviceUrlPattern,
        int connectTimeoutMs,
        int readTimeoutMs,
        int maxConnections,
        int maxIdleTimeSeconds
) {
}
