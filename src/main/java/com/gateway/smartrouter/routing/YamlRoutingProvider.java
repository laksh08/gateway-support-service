package com.gateway.smartrouter.routing;

import com.gateway.smartrouter.config.RoutesProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads routing mappings from {@code application.yml} at startup and on demand.
 *
 * <h3>YAML route format</h3>
 * <pre>
 * routes:
 *   getCustomer:  customer-service/soap/CustomerService
 *   makePayment:  payment-service/soap/PaymentService
 *   issuePolicy:  policy-service:9091
 *   createOrder:  order-service:9092/orders/create
 * </pre>
 *
 * Each value is parsed into a {@link RouteTarget} by {@link RouteTarget#parse(String)}.
 */
public class YamlRoutingProvider implements RoutingProvider {

    private static final Logger log = LoggerFactory.getLogger(YamlRoutingProvider.class);

    private final RoutesProperties routesProperties;
    private final RoutingCache routingCache;

    public YamlRoutingProvider(RoutesProperties routesProperties, RoutingCache routingCache) {
        this.routesProperties = routesProperties;
        this.routingCache = routingCache;
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
        Map<String, String> raw = routesProperties;
        if (raw.isEmpty()) {
            log.warn("No routes configured in application.yml");
            routingCache.replaceFromStrings(Map.of());
            return;
        }
        try {
            routingCache.replaceFromStrings(new HashMap<>(raw));
            log.info("Loaded {} route(s) from application.yml", raw.size());
            routingCache.getRoutes().forEach((method, target) ->
                    log.debug("  {} → {}", method, target.rawValue()));
        } catch (IllegalArgumentException ex) {
            log.error("Invalid route definition in application.yml: {}", ex.getMessage());
        }
    }
}
