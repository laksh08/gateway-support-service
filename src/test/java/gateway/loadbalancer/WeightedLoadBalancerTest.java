package gateway.loadbalancer;

import gateway.discovery.ServiceInstance;
import gateway.discovery.ServiceMetadata;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeightedLoadBalancerTest {

    private WeightedLoadBalancer lb;

    @BeforeEach
    void setUp() {
        lb = new WeightedLoadBalancer();
    }

    @Test
    void returnsEmptyForEmptyList() {
        Assertions.assertThat(lb.select(List.of())).isEmpty();
    }

    @Test
    void highWeightInstanceReceivesMoreTraffic() {
        ServiceInstance heavy = instance("heavy", 1, 10);
        ServiceInstance light = instance("light", 2, 1);
        List<ServiceInstance> instances = List.of(heavy, light);

        Map<String, Integer> counts = new HashMap<>();
        int trials = 1000;
        for (int i = 0; i < trials; i++) {
            lb.select(instances).ifPresent(s -> counts.merge(s.address(), 1, Integer::sum));
        }

        int heavyCount = counts.getOrDefault("heavy", 0);
        int lightCount = counts.getOrDefault("light", 0);

        // heavy should get ~90% (10/11) ± reasonable tolerance
        Assertions.assertThat(heavyCount).isGreaterThan(trials * 75 / 100);
        Assertions.assertThat(lightCount).isLessThan(trials * 25 / 100);
    }

    @Test
    void equalWeightsDistributeEvenly() {
        List<ServiceInstance> instances = List.of(
                instance("a", 1, 5), instance("b", 2, 5));

        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            lb.select(instances).ifPresent(s -> counts.merge(s.address(), 1, Integer::sum));
        }

        // ~50% each, allow generous tolerance
        Assertions.assertThat(counts.getOrDefault("a", 0)).isBetween(350, 650);
        Assertions.assertThat(counts.getOrDefault("b", 0)).isBetween(350, 650);
    }

    private ServiceInstance instance(String address, int port, int weight) {
        return new ServiceInstance(
                "id-" + port, "svc", address, port, List.of(), ServiceMetadata.empty(), weight, "n", "dc1", "");
    }
}
