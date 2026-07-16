package gateway.loadbalancer;

import gateway.discovery.ServiceInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Weighted random selection using Consul service weights.
 *
 * <p>Instances with higher {@link ServiceInstance#weight()} receive proportionally more traffic.
 * The weight comes from the Consul service registration's {@code Weights.Passing} value, which is
 * set per service in the Nomad job spec or Consul service file.
 *
 * <p>Example: if instance A has weight 10 and instance B has weight 5, A receives ~67% of traffic.
 */
@Component("weightedLoadBalancer")
public class WeightedLoadBalancer implements LoadBalancer {

    @Override
    public Optional<ServiceInstance> select(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            return Optional.empty();
        }

        int totalWeight = instances.stream().mapToInt(i -> Math.max(1, i.weight())).sum();
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int accumulated = 0;

        for (ServiceInstance instance : instances) {
            accumulated += Math.max(1, instance.weight());
            if (roll < accumulated) {
                return Optional.of(instance);
            }
        }
        // Fallback (should never reach here if weights sum correctly)
        return Optional.of(instances.getLast());
    }

    @Override
    public String name() {
        return "weighted";
    }
}
