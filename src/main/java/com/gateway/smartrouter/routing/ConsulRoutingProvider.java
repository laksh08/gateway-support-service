package com.gateway.smartrouter.routing;

import com.gateway.smartrouter.config.RoutingProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads routing mappings from Consul KV.
 *
 * <p>Extension points for production Consul integration:
 * <ul>
 *   <li>{@link #fetchRoutesFromConsul()} — implement Consul KV read (e.g. via consul-api client)</li>
 *   <li>{@link #parseConsulValue(String)} — parse KV value into route entries</li>
 * </ul>
 */
public class ConsulRoutingProvider implements RoutingProvider {

    private static final Logger log = LoggerFactory.getLogger(ConsulRoutingProvider.class);

    private final RoutingProperties routingProperties;
    private final RoutingCache routingCache;

    public ConsulRoutingProvider(RoutingProperties routingProperties, RoutingCache routingCache) {
        this.routingProperties = routingProperties;
        this.routingCache = routingCache;
    }

    @PostConstruct
    @Override
    public void initialize() {
        reload();
    }

    @Override
    public String resolveService(String serviceMethod) {
        return routingCache.resolve(serviceMethod);
    }

    @Override
    public void reload() {
        Map<String, String> routes = fetchRoutesFromConsul();
        routingCache.replaceRoutes(routes);
        log.info("Loaded {} route(s) from Consul KV prefix '{}'",
                routes.size(), routingProperties.consul().keyPrefix());
    }

    @Scheduled(fixedDelayString = "${routing.consul.poll-interval-ms:30000}")
    public void scheduledReload() {
        reload();
    }

    /**
     * Extension point: replace with a real Consul KV client.
     *
     * <pre>
     * Example integration:
     *   ConsulClient client = new ConsulClient(consulHost, consulPort);
     *   Response&lt;GetValue&gt; response = client.getKVValue(keyPrefix);
     *   return parseConsulValue(response.getValue().getDecodedValue());
     * </pre>
     */
    protected Map<String, String> fetchRoutesFromConsul() {
        String keyPrefix = routingProperties.consul().keyPrefix();
        log.debug("Fetching routes from Consul KV at prefix: {}", keyPrefix);

        // Placeholder: return default routes until Consul KV client is integrated
        return parseConsulValue("""
                getCustomer=customer-service
                createCustomer=customer-write-service
                makePayment=payment-service
                issuePolicy=policy-service
                """);
    }

    /**
     * Extension point: customize KV value parsing (JSON, YAML, properties format, etc.).
     */
    protected Map<String, String> parseConsulValue(String value) {
        Map<String, String> routes = new HashMap<>();
        if (value == null || value.isBlank()) {
            return routes;
        }
        for (String line : value.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int separator = trimmed.indexOf('=');
            if (separator > 0) {
                routes.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
            }
        }
        return routes;
    }
}
