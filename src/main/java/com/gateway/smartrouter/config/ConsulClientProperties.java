package com.gateway.smartrouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Consul HTTP API client configuration.
 */
@ConfigurationProperties(prefix = "consul.client")
public record ConsulClientProperties(
        String host,
        int port,
        String scheme,
        String token,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
