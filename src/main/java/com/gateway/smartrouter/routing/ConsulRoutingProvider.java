package com.gateway.smartrouter.routing;

import com.gateway.smartrouter.config.RoutingProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;

/**
 * Loads routing mappings from Consul KV via the {@link ConsulKvClient}.
 *
 * <h3>Consul KV layout</h3>
 * Each SOAP service method is stored as a key under the configured prefix:
 * <pre>
 *   KEY:   gateway/routes/getCustomer
 *   VALUE: customer-service/soap/CustomerService
 *
 *   KEY:   gateway/routes/makePayment
 *   VALUE: payment-service:9091/soap/PaymentService
 * </pre>
 *
 * <h3>Updating routes at runtime</h3>
 * Write a new value via the Consul CLI or HTTP API:
 * <pre>
 *   consul kv put gateway/routes/getCustomer "customer-service-v2/soap/CustomerService"
 * </pre>
 * The change takes effect after the next scheduled poll ({@code routing.consul.poll-interval-ms})
 * or immediately via {@code POST /api/portal/routes/refresh}.
 */
public class ConsulRoutingProvider implements RoutingProvider {

    private static final Logger log = LoggerFactory.getLogger(ConsulRoutingProvider.class);

    private final RoutingProperties routingProperties;
    private final RoutingCache routingCache;
    private final ConsulKvClient consulKvClient;

    public ConsulRoutingProvider(
            RoutingProperties routingProperties,
            RoutingCache routingCache,
            ConsulKvClient consulKvClient) {
        this.routingProperties = routingProperties;
        this.routingCache = routingCache;
        this.consulKvClient = consulKvClient;
    }

    @PostConstruct
    @Override
    public void initialize() {
        reload();
    }

    @Override
    public RouteTarget resolveTarget(String serviceMethod) {
        return routingCache.resolve(serviceMethod);
    }

    @Override
    public Map<String, RouteTarget> getAllRoutes() {
        return routingCache.getRoutes();
    }

    @Override
    public void reload() {
        String prefix = routingProperties.consul().keyPrefix();
        consulKvClient.fetchRoutes(prefix)
                .subscribe(routes -> {
                    if (routes.isEmpty()) {
                        log.warn("No routes found in Consul KV at prefix '{}' — keeping existing routes", prefix);
                    } else {
                        routingCache.replaceFromStrings(routes);
                        log.info("Loaded {} route(s) from Consul KV prefix '{}'", routes.size(), prefix);
                        routingCache.getRoutes().forEach((method, target) ->
                                log.debug("  {} → {}", method, target.rawValue()));
                    }
                }, error -> log.error("Failed to reload routes from Consul KV", error));
    }

    @Scheduled(fixedDelayString = "${routing.consul.poll-interval-ms:30000}")
    public void scheduledReload() {
        reload();
    }

    /**
     * Writes or updates a single route in Consul KV and refreshes the local cache.
     */
    public void putRoute(String serviceMethod, String routeValue) {
        String prefix = routingProperties.consul().keyPrefix();
        consulKvClient.putRoute(prefix, serviceMethod, routeValue)
                .doOnSuccess(v -> reload())
                .subscribe(null, error -> log.error("Failed to write route to Consul KV", error));
    }

    /**
     * Deletes a route from Consul KV and refreshes the local cache.
     */
    public void deleteRoute(String serviceMethod) {
        String prefix = routingProperties.consul().keyPrefix();
        consulKvClient.deleteRoute(prefix, serviceMethod)
                .doOnSuccess(v -> reload())
                .subscribe(null, error -> log.error("Failed to delete route from Consul KV", error));
    }
}
