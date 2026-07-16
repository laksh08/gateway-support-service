package gateway.loadbalancer;

import gateway.discovery.ServiceInstance;
import java.util.List;
import java.util.Optional;

/**
 * Selects one healthy service instance from a candidate list.
 *
 * <p>Implementations must be thread-safe — the same instance is shared across thousands of
 * concurrent reactive pipelines without external synchronisation.
 */
public interface LoadBalancer {

    /**
     * Selects an instance from the provided list.
     *
     * @param instances healthy instances returned by {@link gateway.discovery.ServiceDiscovery}
     * @return the selected instance, or {@link Optional#empty()} if the list is empty
     */
    Optional<ServiceInstance> select(List<ServiceInstance> instances);

    /** Human-readable strategy name used in logs and metrics. */
    String name();
}
