package gateway.loadbalancer;

import gateway.discovery.ServiceInstance;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Distributes requests evenly across all healthy instances using a lock-free counter.
 *
 * <p>The {@link AtomicLong} counter is safe to use from reactive pipelines; no locks are used.
 * Counter overflow wraps naturally — the absolute value is taken to keep the modulo positive.
 */
@Component("roundRobinLoadBalancer")
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public Optional<ServiceInstance> select(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return Optional.empty();
        }
        long index = Math.abs(counter.getAndIncrement()) % instances.size();
        return Optional.of(instances.get((int) index));
    }

    @Override
    public String name() {
        return "round-robin";
    }
}
