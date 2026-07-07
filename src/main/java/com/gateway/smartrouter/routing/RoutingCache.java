package com.gateway.smartrouter.routing;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Immutable in-memory routing cache. Maps are replaced atomically, never mutated in place.
 */
@Component
public class RoutingCache {

    private final AtomicReference<Map<String, String>> routes =
            new AtomicReference<>(Map.of());

    public Map<String, String> getRoutes() {
        return routes.get();
    }

    public void replaceRoutes(Map<String, String> newRoutes) {
        routes.set(Collections.unmodifiableMap(Map.copyOf(newRoutes)));
    }

    public String resolve(String serviceMethod) {
        return routes.get().get(serviceMethod);
    }
}
