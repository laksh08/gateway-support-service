package com.gateway.smartrouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "routing")
public record RoutingProperties(
        String provider,
        ConsulProperties consul
) {

    public record ConsulProperties(
            String keyPrefix,
            long pollIntervalMs
    ) {
    }
}
