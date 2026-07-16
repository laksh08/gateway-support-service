package gateway.loadbalancer;

import gateway.discovery.ServiceInstance;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Randomly selects a healthy instance on each request.
 *
 * <p>Uses {@link ThreadLocalRandom} to avoid contention between virtual or platform threads.
 */
@Component("randomLoadBalancer")
public class RandomLoadBalancer implements LoadBalancer {

    @Override
    public Optional<ServiceInstance> select(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return Optional.empty();
        }
        int index = ThreadLocalRandom.current().nextInt(instances.size());
        return Optional.of(instances.get(index));
    }

    @Override
    public String name() {
        return "random";
    }
}
