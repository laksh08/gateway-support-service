package com.gateway.smartrouter.routing;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe in-memory routing cache.
 * Maps SOAP service method names → {@link RouteTarget}.
 * The map is replaced atomically; it is never mutated in place.
 */
@Component
public class RoutingCache {

    private final AtomicReference<Map<String, RouteTarget>> routes =
            new AtomicReference<>(Map.of());

    public Map<String, RouteTarget> getRoutes() {
        return routes.get();
    }

    /**
     * Atomically replaces the entire routing table with a fresh immutable copy.
     *
     * @param newRoutes map of serviceMethod → raw route strings (parsed internally)
     */
    public void replaceFromStrings(Map<String, String> newRoutes) {
        Map<String, RouteTarget> parsed = newRoutes.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> RouteTarget.parse(e.getValue())
                ));
        routes.set(Collections.unmodifiableMap(parsed));
    }

    /**
     * Atomically replaces the entire routing table with pre-parsed targets.
     */
    public void replaceTargets(Map<String, RouteTarget> newRoutes) {
        routes.set(Collections.unmodifiableMap(Map.copyOf(newRoutes)));
    }

    /**
     * Resolves a service method to its {@link RouteTarget}, or {@code null} if not found.
     */
    public RouteTarget resolve(String serviceMethod) {
        return routes.get().get(serviceMethod);
    }
}
