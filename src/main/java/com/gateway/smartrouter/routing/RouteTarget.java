package com.gateway.smartrouter.routing;

/**
 * A fully parsed route target for a SOAP service method.
 *
 * <p>Route values in YAML / Consul KV follow the format:
 * <pre>
 *   {serviceName}[/{upstreamPath}][:{port}]
 * </pre>
 *
 * <h4>Examples</h4>
 * <ul>
 *   <li>{@code customer-service}                         → Consul DNS, default port, original gateway path</li>
 *   <li>{@code customer-service/soap/CustomerService}    → Consul DNS, default port, override upstream path</li>
 *   <li>{@code customer-service:9091}                    → Consul DNS or Envoy upstream port 9091</li>
 *   <li>{@code customer-service:9091/soap/CustSvc}       → Envoy upstream port 9091, override path</li>
 *   <li>{@code 127.0.0.1:9091/soap/CustSvc}             → Direct Envoy upstream port with path</li>
 * </ul>
 *
 * <h4>Envoy Sidecar Modes</h4>
 * <ul>
 *   <li><b>consul-dns</b> (default) — URL: {@code http://customer-service.service.consul:8080/upstream-path}.
 *       Envoy transparent proxy intercepts outbound traffic and applies mTLS + load balancing.</li>
 *   <li><b>upstream-port</b> — URL: {@code http://127.0.0.1:{port}/upstream-path}.
 *       Each Consul upstream is mapped to a fixed local port in the Envoy sidecar config.
 *       Port comes from the route definition or {@code forwarding.envoy.default-upstream-port}.</li>
 *   <li><b>passthrough</b> — URL: {@code http://{host}:{port}/upstream-path}. No DNS suffix appended.</li>
 * </ul>
 *
 * @param host         Consul service name, DNS hostname, or IP (e.g. {@code customer-service})
 * @param upstreamPath Optional path on the upstream service (e.g. {@code /soap/CustomerService}).
 *                     If null/blank the original gateway request path is forwarded unchanged.
 * @param port         Override port (0 = use default from {@code ForwardingProperties})
 * @param rawValue     Original string from config (used for display / Consul KV write-back)
 */
public record RouteTarget(
        String host,
        String upstreamPath,
        int port,
        String rawValue
) {

    /**
     * Parses a route string in the format {@code {host}[:{port}][/{path}]}.
     *
     * <p>Parsing rules (first-slash wins as the path separator):
     * <ol>
     *   <li>Split on first {@code /} to get hostPart and optional pathPart.</li>
     *   <li>If hostPart contains {@code :}, split into host and port.</li>
     * </ol>
     */
    public static RouteTarget parse(String routeValue) {
        if (routeValue == null || routeValue.isBlank()) {
            throw new IllegalArgumentException("Route value must not be blank");
        }
        String trimmed = routeValue.trim();

        String hostPart;
        String pathPart = null;

        int slashIndex = trimmed.indexOf('/');
        if (slashIndex >= 0) {
            hostPart = trimmed.substring(0, slashIndex);
            pathPart = trimmed.substring(slashIndex); // keeps leading slash
        } else {
            hostPart = trimmed;
        }

        String host;
        int port = 0;

        int colonIndex = hostPart.indexOf(':');
        if (colonIndex >= 0) {
            host = hostPart.substring(0, colonIndex);
            try {
                port = Integer.parseInt(hostPart.substring(colonIndex + 1).trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "Invalid port in route value '" + routeValue + "': " + hostPart.substring(colonIndex + 1));
            }
        } else {
            host = hostPart;
        }

        if (host.isBlank()) {
            throw new IllegalArgumentException("Host must not be blank in route value: " + routeValue);
        }

        return new RouteTarget(host, pathPart, port, trimmed);
    }

    /** Whether an explicit upstream path was configured (as opposed to forwarding the original path). */
    public boolean hasUpstreamPath() {
        return upstreamPath != null && !upstreamPath.isBlank();
    }

    /** Whether an explicit port was configured. */
    public boolean hasPort() {
        return port > 0;
    }

    @Override
    public String toString() {
        return rawValue;
    }
}
