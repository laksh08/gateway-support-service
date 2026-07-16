package gateway.loadbalancer;

import gateway.discovery.ServiceInstance;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Routes each request to the instance with the fewest active in-flight connections.
 *
 * <p>Active connection tracking is done with a {@link ConcurrentHashMap} of
 * {@code serviceId → AtomicLong}. The {@link gateway.proxy.ReactiveProxyService}
 * calls {@link #trackStart} when a request begins and {@link #trackEnd} on completion or error.
 */
@Component("leastConnectionsLoadBalancer")
public class LeastConnectionsLoadBalancer implements LoadBalancer {

    private static final Logger log = LoggerFactory.getLogger(LeastConnectionsLoadBalancer.class);

    private final Map<String, AtomicLong> activeConnections = new ConcurrentHashMap<>();

    @Override
    public Optional<ServiceInstance> select(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return Optional.empty();
        }
        ServiceInstance chosen = instances.stream()
                .min(Comparator.comparingLong(
                        i -> activeConnections.computeIfAbsent(i.serviceId(), k -> new AtomicLong()).get()))
                .orElse(instances.getFirst());
        log.trace(
                "LeastConn selected {} with {} active connections",
                chosen.serviceId(),
                getActiveCount(chosen.serviceId()));
        return Optional.of(chosen);
    }

    /** Called by the proxy service before sending the downstream request. */
    public void trackStart(String serviceId) {
        activeConnections.computeIfAbsent(serviceId, k -> new AtomicLong()).incrementAndGet();
    }

    /** Called by the proxy service when the downstream response completes or errors. */
    public void trackEnd(String serviceId) {
        AtomicLong count = activeConnections.get(serviceId);
        if (count != null && count.get() > 0) {
            count.decrementAndGet();
        }
    }

    public long getActiveCount(String serviceId) {
        AtomicLong count = activeConnections.get(serviceId);
        return count != null ? count.get() : 0;
    }

    @Override
    public String name() {
        return "least-connections";
    }
}
