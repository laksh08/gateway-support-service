package com.gateway.smartrouter.routing;

import java.util.Map;

/**
 * Contract for loading service-method → {@link RouteTarget} routing mappings.
 */
public interface RoutingProvider {

    void initialize();

    /** Returns the {@link RouteTarget} for the given SOAP service method, or {@code null} if unmapped. */
    RouteTarget resolveTarget(String serviceMethod);

    /** Reloads routes from the backing store (YAML / Consul KV). */
    void reload();

    /** Returns a snapshot of all current routes (method → raw value string). */
    Map<String, RouteTarget> getAllRoutes();
}
