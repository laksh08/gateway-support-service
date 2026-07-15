package com.gateway.smartrouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Envoy/Consul-Connect forwarding configuration.
 *
 * <h3>Modes</h3>
 * <dl>
 *   <dt>consul-dns (default)</dt>
 *   <dd>Resolves to {@code http://{serviceName}.{consulDomain}:{port}/path}.
 *       Envoy transparent proxy intercepts outbound and applies mTLS.</dd>
 *
 *   <dt>upstream-port</dt>
 *   <dd>Resolves to {@code http://{envoyProxyHost}:{port}/path}.
 *       Each upstream is mapped to a local Envoy listener port declared in the
 *       service's Consul Connect proxy configuration (or HCL sidecar_service block).</dd>
 *
 *   <dt>passthrough</dt>
 *   <dd>Resolves to {@code http://{host}:{port}/path} with no DNS suffix.
 *       Useful for direct calls or when running outside a mesh.</dd>
 * </dl>
 */
@ConfigurationProperties(prefix = "forwarding.envoy")
public record EnvoyProperties(
        String mode,
        String consulDomain,
        int defaultPort,
        String envoyProxyHost,
        int defaultUpstreamPort
) {
    public static final String MODE_CONSUL_DNS = "consul-dns";
    public static final String MODE_UPSTREAM_PORT = "upstream-port";
    public static final String MODE_PASSTHROUGH = "passthrough";
}
