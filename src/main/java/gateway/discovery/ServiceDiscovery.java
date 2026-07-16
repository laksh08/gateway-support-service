package gateway.discovery;

import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Discovers healthy service instances from a service registry.
 *
 * <p>The contract guarantees:
 * <ul>
 *   <li>Only instances with all health checks passing are returned.
 *   <li>Results may be served from a Caffeine cache for performance (TTL configurable).
 *   <li>Implementations must never block the Netty event loop.
 * </ul>
 */
public interface ServiceDiscovery {

    /**
     * Returns all currently healthy instances of the named service.
     *
     * @param serviceName Consul service name (e.g. {@code customer-service})
     * @return a {@link Flux} of {@link ServiceInstance} records; empty if none are healthy
     */
    Flux<ServiceInstance> discover(String serviceName);

    /**
     * Returns all healthy instances as a single list (convenience wrapper around
     * {@link #discover(String)}).
     */
    default Mono<List<ServiceInstance>> discoverAll(String serviceName) {
        return discover(serviceName).collectList();
    }
}
