package gateway.loadbalancer;

import gateway.discovery.ServiceInstance;
import gateway.discovery.ServiceMetadata;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoundRobinLoadBalancerTest {

    private RoundRobinLoadBalancer lb;

    @BeforeEach
    void setUp() {
        lb = new RoundRobinLoadBalancer();
    }

    @Test
    void returnsEmptyForEmptyList() {
        Assertions.assertThat(lb.select(List.of())).isEmpty();
    }

    @Test
    void returnsSingleInstance() {
        List<ServiceInstance> instances = List.of(instance("10.0.0.1", 8001));
        Assertions.assertThat(lb.select(instances)).isPresent()
                .get()
                .extracting(ServiceInstance::address)
                .isEqualTo("10.0.0.1");
    }

    @Test
    void distributesAcrossAllInstances() {
        List<ServiceInstance> instances = List.of(
                instance("10.0.0.1", 8001),
                instance("10.0.0.2", 8002),
                instance("10.0.0.3", 8003));

        var selected = List.of(
                lb.select(instances).orElseThrow(),
                lb.select(instances).orElseThrow(),
                lb.select(instances).orElseThrow());

        // All three instances should be selected in one full round
        Assertions.assertThat(selected)
                .extracting(ServiceInstance::address)
                .containsExactlyInAnyOrder("10.0.0.1", "10.0.0.2", "10.0.0.3");
    }

    @Test
    void wrapsAroundAfterAllInstances() {
        List<ServiceInstance> instances = List.of(
                instance("a", 1), instance("b", 2));

        String first = lb.select(instances).map(ServiceInstance::address).orElseThrow();
        lb.select(instances);
        String third = lb.select(instances).map(ServiceInstance::address).orElseThrow();

        // Should wrap and return the same as the first selection
        Assertions.assertThat(third).isEqualTo(first);
    }

    @Test
    void strategyNameIsRoundRobin() {
        Assertions.assertThat(lb.name()).isEqualTo("round-robin");
    }

    private ServiceInstance instance(String address, int port) {
        return new ServiceInstance(
                "id-" + port, "svc", address, port, List.of(), ServiceMetadata.empty(), 1, "n", "dc1", "");
    }
}
