package com.gateway.smartrouter.routing;

import java.util.Map;

/**
 * Contract for loading service-method to downstream-service routing mappings.
 */
public interface RoutingProvider {

    void initialize();

    String resolveService(String serviceMethod);

    void reload();
}
