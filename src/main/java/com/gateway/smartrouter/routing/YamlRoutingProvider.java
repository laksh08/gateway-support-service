package com.gateway.smartrouter.routing;

import com.gateway.smartrouter.config.RoutesProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads routing mappings from application.yml at startup and on manual reload.
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
    public String resolveService(String serviceMethod) {
        return routingCache.resolve(serviceMethod);
    }

    @Override
    public void reload() {
        Map<String, String> routes = routesProperties;
        if (routes.isEmpty()) {
            log.warn("No routes configured in application.yml");
            routingCache.replaceRoutes(Map.of());
            return;
        }
        routingCache.replaceRoutes(new HashMap<>(routes));
        log.info("Loaded {} route(s) from application.yml", routes.size());
    }
}
